package com.pobar.security;

public final class AuthTokens {
    private AuthTokens() {}

    public static String extractBearer(String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7);
        }
        return null;
    }
}
