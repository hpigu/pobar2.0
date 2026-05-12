package com.pobar.service;

import com.pobar.dto.auth.ChangePasswordRequest;
import com.pobar.dto.auth.LoginRequest;
import com.pobar.dto.auth.LoginResponse;

public interface AuthService {

    LoginResponse login(LoginRequest request, String clientIp, String userAgent);

    /** 用 refresh token 換新 access token + rotated refresh token */
    LoginResponse refresh(String refreshToken, String clientIp, String userAgent);

    /** 登出：撤銷此 refresh token + 將 access token 加入黑名單 */
    void logout(String accessToken, String refreshToken);

    /**
     * 修改密碼。成功後撤銷該使用者所有 refresh token + 將舊 access token 加入黑名單，
     * 並回傳全新的 access + refresh token。
     */
    LoginResponse changePassword(Integer userId, ChangePasswordRequest request,
                                 String oldAccessToken, String clientIp, String userAgent);
}
