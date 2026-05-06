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

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    // 每個 IP 每分鐘最多 60 次請求（一般端點）
    private static final int GENERAL_LIMIT = 60;
    // 訂位端點每個 IP 每小時最多 10 次
    private static final int RESERVATION_LIMIT = 10;

    private final Map<String, Bucket> generalBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> reservationBuckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String ip = getClientIp(request);
        String path = request.getRequestURI();

        Bucket bucket;
        if (path.equals("/api/reservations") && "POST".equals(request.getMethod())) {
            bucket = reservationBuckets.computeIfAbsent(ip,
                    k -> Bucket.builder()
                            .addLimit(Bandwidth.simple(RESERVATION_LIMIT, Duration.ofHours(1)))
                            .build());
        } else {
            bucket = generalBuckets.computeIfAbsent(ip,
                    k -> Bucket.builder()
                            .addLimit(Bandwidth.simple(GENERAL_LIMIT, Duration.ofMinutes(1)))
                            .build());
        }

        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
        } else {
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"code\":429,\"message\":\"請求過於頻繁，請稍後再試\",\"data\":null}");
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
