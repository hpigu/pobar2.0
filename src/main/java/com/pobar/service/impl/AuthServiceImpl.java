package com.pobar.service.impl;

import com.pobar.dto.auth.LoginRequest;
import com.pobar.dto.auth.LoginResponse;
import com.pobar.entity.JwtBlacklist;
import com.pobar.entity.LoginAttempt;
import com.pobar.entity.User;
import com.pobar.exception.BusinessException;
import com.pobar.mapper.JwtBlacklistMapper;
import com.pobar.mapper.LoginAttemptMapper;
import com.pobar.mapper.UserMapper;
import com.pobar.security.JwtUtil;
import com.pobar.logging.Audit;
import com.pobar.service.AuthService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final int MAX_FAIL = 5;
    private static final int LOCK_MINUTES = 15;

    private final UserMapper userMapper;
    private final LoginAttemptMapper loginAttemptMapper;
    private final JwtBlacklistMapper jwtBlacklistMapper;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    @Audit(action = "LOGIN")
    public LoginResponse login(LoginRequest request) {
        checkLocked(request.getAccount());

        User user = userMapper.findByAccount(request.getAccount());
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            recordFail(request.getAccount());
            throw new BusinessException(401, "帳號或密碼錯誤");
        }

        resetFail(request.getAccount());
        String token = jwtUtil.generate(user.getId(), user.getAccount(), user.getRole());

        return LoginResponse.builder()
                .token(token)
                .userId(user.getId())
                .account(user.getAccount())
                .role(user.getRole())
                .build();
    }

    @Override
    @Transactional
    @Audit(action = "LOGOUT")
    public void logout(String token) {
        if (token == null || !jwtUtil.isValid(token)) return;

        Claims claims = jwtUtil.parse(token);
        JwtBlacklist blacklist = new JwtBlacklist();
        blacklist.setTokenHash(User.hashToken(token));
        blacklist.setExpiresAt(claims.getExpiration().toInstant()
                .atZone(java.time.ZoneId.systemDefault()).toLocalDateTime());
        blacklist.setCreatedAt(LocalDateTime.now());
        jwtBlacklistMapper.insert(blacklist);
    }

    private void checkLocked(String account) {
        LoginAttempt attempt = loginAttemptMapper.findByAccount(account);
        if (attempt != null && attempt.getLockedUntil() != null
                && attempt.getLockedUntil().isAfter(LocalDateTime.now())) {
            throw new BusinessException(429, "帳號已鎖定，請 " + LOCK_MINUTES + " 分鐘後再試");
        }
    }

    private void recordFail(String account) {
        LoginAttempt attempt = loginAttemptMapper.findByAccount(account);
        if (attempt == null) {
            attempt = new LoginAttempt();
            attempt.setAccount(account);
            attempt.setFailCount(1);
            attempt.setUpdatedAt(LocalDateTime.now());
            loginAttemptMapper.insert(attempt);
        } else {
            int count = attempt.getFailCount() + 1;
            attempt.setFailCount(count);
            attempt.setLockedUntil(count >= MAX_FAIL
                    ? LocalDateTime.now().plusMinutes(LOCK_MINUTES) : null);
            attempt.setUpdatedAt(LocalDateTime.now());
            loginAttemptMapper.updateById(attempt);
        }
    }

    private void resetFail(String account) {
        LoginAttempt attempt = loginAttemptMapper.findByAccount(account);
        if (attempt != null) {
            attempt.setFailCount(0);
            attempt.setLockedUntil(null);
            attempt.setUpdatedAt(LocalDateTime.now());
            loginAttemptMapper.updateById(attempt);
        }
    }
}
