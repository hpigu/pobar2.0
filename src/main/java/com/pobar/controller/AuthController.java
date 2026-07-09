package com.pobar.controller;

import com.pobar.common.Result;
import com.pobar.dto.auth.ChangePasswordRequest;
import com.pobar.dto.auth.LoginRequest;
import com.pobar.dto.auth.LoginResponse;
import com.pobar.dto.auth.RefreshRequest;
import com.pobar.security.AuthTokens;
import com.pobar.security.AuthUser;
import com.pobar.security.ClientIpResolver;
import com.pobar.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                       HttpServletRequest httpRequest) {
        return Result.ok(authService.login(request, getClientIp(httpRequest), getUserAgent(httpRequest)));
    }

    /** 用 refresh token 換新 access + rotated refresh token（公開端點） */
    @PostMapping("/refresh")
    public Result<LoginResponse> refresh(@Valid @RequestBody RefreshRequest request,
                                          HttpServletRequest httpRequest) {
        return Result.ok(authService.refresh(request.getRefreshToken(),
                getClientIp(httpRequest), getUserAgent(httpRequest)));
    }

    @PostMapping("/logout")
    public Result<?> logout(@RequestHeader(value = "Authorization", required = false) String authHeader,
                            @RequestBody(required = false) RefreshRequest request) {
        String accessToken = AuthTokens.extractBearer(authHeader);
        String refreshToken = request != null ? request.getRefreshToken() : null;
        authService.logout(accessToken, refreshToken);
        return Result.ok();
    }

    @PostMapping("/change-password")
    public Result<LoginResponse> changePassword(@Valid @RequestBody ChangePasswordRequest request,
                                                @RequestHeader(value = "Authorization", required = false) String authHeader,
                                                HttpServletRequest httpRequest) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer userId = ((AuthUser) auth.getPrincipal()).id();
        String oldToken = AuthTokens.extractBearer(authHeader);
        return Result.ok(authService.changePassword(userId, request, oldToken,
                getClientIp(httpRequest), getUserAgent(httpRequest)));
    }

    @GetMapping("/me")
    public Result<?> me() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        AuthUser u = (AuthUser) auth.getPrincipal();
        return Result.ok(java.util.Map.of(
                "userId", u.id(),
                "account", u.account(),
                "role", u.role()
        ));
    }

    private String getClientIp(HttpServletRequest request) {
        return ClientIpResolver.resolve(request);
    }

    private String getUserAgent(HttpServletRequest request) {
        return request.getHeader("User-Agent");
    }
}
