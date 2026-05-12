package com.pobar.scheduler;

import com.pobar.mapper.IpLockoutMapper;
import com.pobar.mapper.JwtBlacklistMapper;
import com.pobar.mapper.RefreshTokenMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 認證相關過期資料清理：
 * - jwt_blacklist：清過期 token
 * - ip_lockout：清過期鎖定
 * - refresh_token：清過期或已撤銷 7 天以上的
 *
 * 每日 04:00 執行（業務日重置後 1 小時，避開備份排程 03:00）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthCleanupScheduler {

    private final JwtBlacklistMapper jwtBlacklistMapper;
    private final IpLockoutMapper ipLockoutMapper;
    private final RefreshTokenMapper refreshTokenMapper;

    @Scheduled(cron = "0 0 4 * * *", zone = "Asia/Taipei")
    public void cleanup() {
        try {
            int blacklist = jwtBlacklistMapper.deleteExpired();
            int lockouts = ipLockoutMapper.deleteExpired();
            int refreshTokens = refreshTokenMapper.deleteExpiredOrOldRevoked();
            log.info("[AUTH-CLEANUP] 清除過期 JWT 黑名單 {} 筆、過期 IP 鎖 {} 筆、過期/已撤銷 refresh token {} 筆",
                    blacklist, lockouts, refreshTokens);
        } catch (Exception e) {
            log.error("[AUTH-CLEANUP] 清理失敗", e);
        }
    }
}
