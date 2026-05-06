package com.pobar.dto.auth;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginResponse {

    private String token;
    private Integer userId;
    private String account;
    private String role;
}
