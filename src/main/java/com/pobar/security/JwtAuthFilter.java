package com.pobar.security;

import com.pobar.common.ErrorCode;
import com.pobar.entity.User;
import com.pobar.mapper.JwtBlacklistMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final JwtBlacklistMapper jwtBlacklistMapper;

    /** 持有 must_change_password 旗標的 token 只能存取這幾個端點 */
    private static final Set<String> MCP_ALLOWED_PATHS = Set.of(
            "/api/auth/change-password",
            "/api/auth/logout",
            "/api/auth/me"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String token = extractToken(request);

        if (token != null && jwtUtil.isValid(token) && !isBlacklisted(token)) {
            // ── 強制改密碼旗標 ──
            if (jwtUtil.mustChangePassword(token) && !MCP_ALLOWED_PATHS.contains(request.getRequestURI())) {
                writeMustChangePasswordResponse(response);
                return;
            }

            int userId = jwtUtil.getUserId(token);
            String account = jwtUtil.getAccount(token);
            String role = jwtUtil.getRole(token);

            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    new AuthUser(userId, account, role),
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + role))
            );
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        chain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        return AuthTokens.extractBearer(request.getHeader("Authorization"));
    }

    private boolean isBlacklisted(String token) {
        String hash = User.hashToken(token);
        return jwtBlacklistMapper.existsByTokenHash(hash);
    }

    private void writeMustChangePasswordResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(
                "{\"code\":" + ErrorCode.PASSWORD_MUST_CHANGE
                        + ",\"message\":\"請先修改密碼\",\"data\":null}");
    }
}
