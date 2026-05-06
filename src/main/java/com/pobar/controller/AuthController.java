package com.pobar.controller;

import com.pobar.common.Result;
import com.pobar.dto.auth.LoginRequest;
import com.pobar.dto.auth.LoginResponse;
import com.pobar.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.ok(authService.login(request));
    }

    @PostMapping("/logout")
    public Result<?> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            authService.logout(authHeader.substring(7));
        }
        return Result.ok();
    }

    @GetMapping("/me")
    public Result<?> me(@RequestHeader("Authorization") String authHeader) {
        return Result.ok();
    }
}
