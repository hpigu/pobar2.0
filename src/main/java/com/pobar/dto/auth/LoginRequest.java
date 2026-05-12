package com.pobar.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "帳號不得為空")
    private String account;

    @NotBlank(message = "密碼不得為空")
    private String password;

    /** 本機記住：true 時 refresh token TTL 加長（從預設 7 天延長到 30 天） */
    private Boolean rememberDevice;
}
