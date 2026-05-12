package com.pobar.dto.auth;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginResponse {

    /** Access token（短 TTL，所有 API 用此） */
    private String token;
    /** Refresh token（長 TTL，只能呼叫 /api/auth/refresh 換新 access token） */
    private String refreshToken;
    private Integer userId;
    private String account;
    private String role;
    /** true 時前端應導向改密碼頁；改完密碼前不應呼叫其他 API */
    private Boolean mustChangePassword;
}
