package com.pobar.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RefreshRequest {

    @NotBlank(message = "refresh token 不得為空")
    private String refreshToken;
}
