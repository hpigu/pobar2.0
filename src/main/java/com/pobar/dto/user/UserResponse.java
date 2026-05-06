package com.pobar.dto.user;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserResponse {

    private Integer id;
    private String account;
    private String email;
    private String phone;
    private String role;
    private Integer isActive;
    private LocalDateTime createdAt;
}
