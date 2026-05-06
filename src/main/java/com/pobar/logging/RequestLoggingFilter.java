package com.pobar.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * HTTP 層 Log：記錄每個 API 請求的 IP、路徑、耗時、回應狀態。
 * 不記錄 request body，避免密碼等敏感資料外洩。
 */
@Slf4j
@Component
@Order(1)
public class RequestLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        long start = System.currentTimeMillis();
        String ip = getClientIp(request);
        String method = request.getMethod();
        String uri = request.getRequestURI();

        try {
            chain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - start;
            int status = response.getStatus();

            if (status >= 500) {
                log.error("[HTTP] {} {} {} {}ms ip={}", method, uri, status, duration, ip);
            } else if (status >= 400) {
                log.warn("[HTTP] {} {} {} {}ms ip={}", method, uri, status, duration, ip);
            } else {
                log.info("[HTTP] {} {} {} {}ms ip={}", method, uri, status, duration, ip);
            }
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        // 靜態資源不記錄
        return uri.startsWith("/uploads/") || uri.startsWith("/actuator/");
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
