package com.pobar.service.impl;

import com.pobar.common.ErrorCode;
import com.pobar.dto.auth.ChangePasswordRequest;
import com.pobar.dto.auth.LoginRequest;
import com.pobar.dto.auth.LoginResponse;
import com.pobar.entity.IpLockout;
import com.pobar.entity.JwtBlacklist;
import com.pobar.entity.LoginAttempt;
import com.pobar.entity.RefreshToken;
import com.pobar.entity.User;
import com.pobar.exception.BusinessException;
import com.pobar.logging.Audit;
import com.pobar.mapper.IpLockoutMapper;
import com.pobar.mapper.JwtBlacklistMapper;
import com.pobar.mapper.LoginAttemptMapper;
import com.pobar.mapper.RefreshTokenMapper;
import com.pobar.mapper.UserMapper;
import com.pobar.security.JwtUtil;
import com.pobar.service.AuthService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    /** 同 (account, ip) 5 次失敗 → 鎖該組合 15 分鐘 */
    private static final int ACCOUNT_IP_MAX_FAIL = 5;
    private static final int ACCOUNT_IP_LOCK_MINUTES = 15;

    /** 同 IP 30 分鐘內累計失敗 20 次 → 鎖 IP 1 小時（防分散式暴力破解） */
    private static final int IP_MAX_FAIL_IN_WINDOW = 20;
    private static final int IP_WINDOW_MINUTES = 30;
    private static final int IP_LOCK_MINUTES = 60;

    private final UserMapper userMapper;
    private final LoginAttemptMapper loginAttemptMapper;
    private final IpLockoutMapper ipLockoutMapper;
    private final JwtBlacklistMapper jwtBlacklistMapper;
    private final RefreshTokenMapper refreshTokenMapper;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    @Value("${jwt.refresh-standard-days:7}")
    private int refreshStandardDays;

    @Value("${jwt.refresh-trusted-days:30}")
    private int refreshTrustedDays;

    // ════════════════════════════ login ════════════════════════════

    @Override
    @Transactional
    @Audit(action = "LOGIN", entityType = "User",
            entityIdExpr = "#result?.userId",
            detailExpr = "'account=' + #request.account + ', ip=' + #clientIp + ', trusted=' + (#request.rememberDevice == true)")
    public LoginResponse login(LoginRequest request, String clientIp, String userAgent) {
        checkIpLocked(clientIp);
        checkAccountIpLocked(request.getAccount(), clientIp);

        User user = userMapper.findByAccount(request.getAccount());
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            recordFail(request.getAccount(), clientIp);
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS, "帳號或密碼錯誤");
        }
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new BusinessException(ErrorCode.ACCOUNT_INACTIVE, "帳號已被停用");
        }

        resetFail(request.getAccount(), clientIp);

        boolean mustChange = Boolean.TRUE.equals(user.getMustChangePassword());
        boolean trusted = Boolean.TRUE.equals(request.getRememberDevice());

        String accessToken = jwtUtil.generate(user.getId(), user.getAccount(), user.getRole(), mustChange);
        // 強制改密碼狀態時不發 refresh token，避免在未改密碼前就能 refresh 拿到正常 token
        String refreshToken = mustChange ? null : issueRefreshToken(user.getId(), trusted, clientIp, userAgent);

        return LoginResponse.builder()
                .token(accessToken)
                .refreshToken(refreshToken)
                .userId(user.getId())
                .account(user.getAccount())
                .role(user.getRole())
                .mustChangePassword(mustChange)
                .build();
    }

    // ════════════════════════════ refresh ════════════════════════════

    @Override
    @Transactional
    public LoginResponse refresh(String refreshToken, String clientIp, String userAgent) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID, "缺少 refresh token");
        }
        String hash = User.hashToken(refreshToken);
        RefreshToken rt = refreshTokenMapper.findActive(hash);
        if (rt == null) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID, "refresh token 無效或已失效");
        }
        User user = userMapper.selectById(rt.getUserId());
        if (user == null || !Boolean.TRUE.equals(user.getIsActive())) {
            throw new BusinessException(ErrorCode.ACCOUNT_INACTIVE, "帳號已被停用");
        }

        boolean trusted = Boolean.TRUE.equals(rt.getTrusted());

        // Rotation：撤銷舊 refresh + 發新 refresh（同時阻擋舊 refresh 被重放）
        refreshTokenMapper.revokeByHash(hash);
        String newRefresh = issueRefreshToken(user.getId(), trusted, clientIp, userAgent);

        // 注意：refresh 流程不會帶 mcp 旗標（在 login 時 mustChange=true 就不發 refresh，所以走到這就是 mcp=false）
        String newAccess = jwtUtil.generate(user.getId(), user.getAccount(), user.getRole(), false);

        return LoginResponse.builder()
                .token(newAccess)
                .refreshToken(newRefresh)
                .userId(user.getId())
                .account(user.getAccount())
                .role(user.getRole())
                .mustChangePassword(false)
                .build();
    }

    // ════════════════════════════ logout ════════════════════════════

    @Override
    @Transactional
    @Audit(action = "LOGOUT", entityType = "User")
    public void logout(String accessToken, String refreshToken) {
        // 1. access token 進黑名單
        if (accessToken != null && jwtUtil.isValid(accessToken)) {
            Claims claims = jwtUtil.parse(accessToken);
            JwtBlacklist bl = new JwtBlacklist();
            bl.setTokenHash(User.hashToken(accessToken));
            bl.setExpiresAt(claims.getExpiration().toInstant()
                    .atZone(java.time.ZoneId.systemDefault()).toLocalDateTime());
            bl.setCreatedAt(LocalDateTime.now());
            jwtBlacklistMapper.insert(bl);
        }
        // 2. refresh token 撤銷
        if (refreshToken != null && !refreshToken.isBlank()) {
            refreshTokenMapper.revokeByHash(User.hashToken(refreshToken));
        }
    }

    // ════════════════════════════ change password ════════════════════════════

    @Override
    @Transactional
    @Audit(action = "CHANGE_PASSWORD", entityType = "User",
            entityIdExpr = "#userId",
            detailExpr = "'account=' + #result?.account")
    public LoginResponse changePassword(Integer userId, ChangePasswordRequest request,
                                         String oldAccessToken, String clientIp, String userAgent) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "使用者不存在");
        }
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.OLD_PASSWORD_WRONG, "舊密碼錯誤");
        }
        if (request.getOldPassword().equals(request.getNewPassword())) {
            throw new BusinessException(ErrorCode.NEW_PASSWORD_WEAK, "新密碼不可與舊密碼相同");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setMustChangePassword(false);
        userMapper.updateById(user);

        // 改密碼後撤銷該使用者所有 refresh token + 舊 access token 進黑名單
        refreshTokenMapper.revokeAllByUserId(userId);
        if (oldAccessToken != null && jwtUtil.isValid(oldAccessToken)) {
            logout(oldAccessToken, null);
        }

        // 簽發全新的雙 token（mcp=false，視為新登入）
        String newAccess = jwtUtil.generate(user.getId(), user.getAccount(), user.getRole(), false);
        String newRefresh = issueRefreshToken(user.getId(), false, clientIp, userAgent);

        return LoginResponse.builder()
                .token(newAccess)
                .refreshToken(newRefresh)
                .userId(user.getId())
                .account(user.getAccount())
                .role(user.getRole())
                .mustChangePassword(false)
                .build();
    }

    // ════════════════════════════ helpers ════════════════════════════

    /** 產生 + 持久化 refresh token，回傳明文 token（只給呼叫端用一次） */
    private String issueRefreshToken(Integer userId, boolean trusted, String ip, String userAgent) {
        String token = jwtUtil.generateRefreshToken();
        int days = trusted ? refreshTrustedDays : refreshStandardDays;

        RefreshToken rt = new RefreshToken();
        rt.setUserId(userId);
        rt.setTokenHash(User.hashToken(token));
        rt.setExpiresAt(LocalDateTime.now().plusDays(days));
        rt.setTrusted(trusted);
        rt.setRevoked(false);
        rt.setIp(ip);
        rt.setUserAgent(truncate(userAgent, 255));
        rt.setCreatedAt(LocalDateTime.now());
        rt.setLastUsedAt(LocalDateTime.now());
        refreshTokenMapper.insert(rt);
        return token;
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }

    private void checkIpLocked(String ip) {
        IpLockout lockout = ipLockoutMapper.findByIp(ip);
        if (lockout != null && lockout.getLockedUntil().isAfter(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.IP_LOCKED,
                    "來源 IP 已被鎖定，請 " + IP_LOCK_MINUTES + " 分鐘後再試");
        }
    }

    private void checkAccountIpLocked(String account, String ip) {
        LoginAttempt attempt = loginAttemptMapper.findByAccountAndIp(account, ip);
        if (attempt != null && attempt.getLockedUntil() != null
                && attempt.getLockedUntil().isAfter(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED,
                    "帳號已鎖定，請 " + ACCOUNT_IP_LOCK_MINUTES + " 分鐘後再試");
        }
    }

    private void recordFail(String account, String ip) {
        LoginAttempt attempt = loginAttemptMapper.findByAccountAndIp(account, ip);
        if (attempt == null) {
            attempt = new LoginAttempt();
            attempt.setAccount(account);
            attempt.setIp(ip);
            attempt.setFailCount(1);
            attempt.setUpdatedAt(LocalDateTime.now());
            loginAttemptMapper.insert(attempt);
        } else {
            int count = attempt.getFailCount() + 1;
            attempt.setFailCount(count);
            attempt.setLockedUntil(count >= ACCOUNT_IP_MAX_FAIL
                    ? LocalDateTime.now().plusMinutes(ACCOUNT_IP_LOCK_MINUTES) : null);
            attempt.setUpdatedAt(LocalDateTime.now());
            loginAttemptMapper.updateById(attempt);
        }
        // IP 維度：過去 30 分鐘內同 IP 失敗總和
        LocalDateTime windowStart = LocalDateTime.now().minusMinutes(IP_WINDOW_MINUTES);
        int ipFails = loginAttemptMapper.sumFailsByIpSince(ip, windowStart);
        if (ipFails >= IP_MAX_FAIL_IN_WINDOW) {
            lockIp(ip, "30分鐘內失敗 " + ipFails + " 次");
        }
    }

    private void lockIp(String ip, String reason) {
        IpLockout existing = ipLockoutMapper.findByIp(ip);
        LocalDateTime lockedUntil = LocalDateTime.now().plusMinutes(IP_LOCK_MINUTES);
        if (existing == null) {
            IpLockout lockout = new IpLockout();
            lockout.setIp(ip);
            lockout.setLockedUntil(lockedUntil);
            lockout.setReason(reason);
            lockout.setCreatedAt(LocalDateTime.now());
            ipLockoutMapper.insert(lockout);
        } else {
            existing.setLockedUntil(lockedUntil);
            existing.setReason(reason);
            ipLockoutMapper.updateById(existing);
        }
    }

    private void resetFail(String account, String ip) {
        LoginAttempt attempt = loginAttemptMapper.findByAccountAndIp(account, ip);
        if (attempt != null) {
            attempt.setFailCount(0);
            attempt.setLockedUntil(null);
            attempt.setUpdatedAt(LocalDateTime.now());
            loginAttemptMapper.updateById(attempt);
        }
    }
}
