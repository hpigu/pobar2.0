package com.pobar.dto.user;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class UserCreateRequest {

    @NotBlank
    @Size(min = 3, max = 30)
    private String account;

    @NotBlank
    @Size(min = 8, max = 72)
    private String password;

    @Email
    private String email;

    @Pattern(regexp = "^09\\d{8}$", message = "手機號碼格式不正確")
    private String phone;

    @NotBlank
    @Pattern(regexp = "ADMIN|MANAGER|WAITER|BARTENDER|KITCHEN", message = "角色不合法")
    private String role;
}
