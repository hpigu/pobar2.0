package com.pobar.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "帳號不得為空")
    private String account;

    @NotBlank(message = "密碼不得為空")
    private String password;
}
