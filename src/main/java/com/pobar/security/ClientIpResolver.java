package com.pobar.security;

import jakarta.servlet.http.HttpServletRequest;

public final class ClientIpResolver {
    private ClientIpResolver() {}

    public static String resolve(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            String[] parts = xff.split(",");
            return parts[parts.length - 1].trim(); // 取最右側 = nginx 附加的可信值
        }
        return req.getRemoteAddr();
    }
}
