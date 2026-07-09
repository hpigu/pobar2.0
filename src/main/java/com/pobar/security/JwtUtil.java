package com.pobar.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;

@Component
public class JwtUtil {

    private final SecretKey key;
    private final long accessExpirationMillis;
    private static final SecureRandom RANDOM = new SecureRandom();

    public JwtUtil(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-expiration-minutes:15}") int accessExpirationMinutes) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpirationMillis = (long) accessExpirationMinutes * 60 * 1000;
    }

    /** 一般登入用（不含強制改密碼旗標）。 */
    public String generate(int userId, String account, String role) {
        return generate(userId, account, role, false);
    }

    /** 含 mustChangePassword 旗標的版本。為 true 時，過濾器會限制可呼叫的路徑。 */
    public String generate(int userId, String account, String role, boolean mustChangePassword) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("account", account)
                .claim("role", role)
                .claim("mcp", mustChangePassword)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + accessExpirationMillis))
                .signWith(key)
                .compact();
    }

    /**
     * 產生 refresh token：opaque 256-bit 隨機字串（base64url）。
     * 不是 JWT；後端會把它 hash 後存 DB。
     */
    public String generateRefreshToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isValid(String token) {
        try {
            parse(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public int getUserId(String token) {
        return Integer.parseInt(parse(token).getSubject());
    }

    public String getRole(String token) {
        return parse(token).get("role", String.class);
    }

    public String getAccount(String token) {
        return parse(token).get("account", String.class);
    }

    /** mcp = must change password */
    public boolean mustChangePassword(String token) {
        Boolean v = parse(token).get("mcp", Boolean.class);
        return v != null && v;
    }
}
