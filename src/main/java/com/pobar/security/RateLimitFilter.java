package com.pobar.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 依路徑分類的 rate limit。
 *
 * 一般端點 ........... 60 req/分鐘/IP
 * 登入端點 ........... 10 req/分鐘/IP（防暴力破解的第一道防線；第二道是 LoginAttempt）
 * 結帳端點 ...........  5 req/分鐘/IP（避免重複扣款）
 * 訂位端點 ........... 10 req/小時/IP
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int GENERAL_LIMIT = 60;
    private static final int LOGIN_LIMIT = 10;
    private static final int CHECKOUT_LIMIT = 5;
    private static final int RESERVATION_LIMIT = 10;

    private final Map<String, Bucket> generalBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> loginBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> checkoutBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> reservationBuckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String path = request.getRequestURI();

        // Actuator 健康檢查由 LB / k8s 高頻打，不該被 rate limit
        if (path.startsWith("/actuator/")) {
            chain.doFilter(request, response);
            return;
        }

        String ip = ClientIpResolver.resolve(request);
        String method = request.getMethod();

        Bucket bucket = pickBucket(ip, path, method);
        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
        } else {
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"code\":429,\"message\":\"請求過於頻繁，請稍後再試\",\"data\":null}");
        }
    }

    private Bucket pickBucket(String ip, String path, String method) {
        // 1. 登入相關（含 login / change-password）
        if (isLoginRelated(path, method)) {
            return loginBuckets.computeIfAbsent(ip,
                    k -> Bucket.builder()
                            .addLimit(Bandwidth.simple(LOGIN_LIMIT, Duration.ofMinutes(1)))
                            .build());
        }
        // 2. 結帳
        if (isCheckout(path, method)) {
            return checkoutBuckets.computeIfAbsent(ip,
                    k -> Bucket.builder()
                            .addLimit(Bandwidth.simple(CHECKOUT_LIMIT, Duration.ofMinutes(1)))
                            .build());
        }
        // 3. 預約建立
        if ("POST".equals(method) && path.equals("/api/reservations")) {
            return reservationBuckets.computeIfAbsent(ip,
                    k -> Bucket.builder()
                            .addLimit(Bandwidth.simple(RESERVATION_LIMIT, Duration.ofHours(1)))
                            .build());
        }
        // 4. 一般
        return generalBuckets.computeIfAbsent(ip,
                k -> Bucket.builder()
                        .addLimit(Bandwidth.simple(GENERAL_LIMIT, Duration.ofMinutes(1)))
                        .build());
    }

    private boolean isLoginRelated(String path, String method) {
        if (!"POST".equals(method)) return false;
        return path.equals("/api/auth/login")
                || path.equals("/api/auth/change-password");
    }

    private boolean isCheckout(String path, String method) {
        return "POST".equals(method)
                && path.startsWith("/api/sessions/") && path.endsWith("/payment");
    }
}
