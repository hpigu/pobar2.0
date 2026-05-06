package com.pobar.service;

import com.pobar.dto.auth.LoginRequest;
import com.pobar.dto.auth.LoginResponse;

public interface AuthService {

    LoginResponse login(LoginRequest request);

    void logout(String token);
}
