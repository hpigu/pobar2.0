package com.pobar.dto.user;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class UserUpdateRequest {

    @Email
    private String email;

    @Pattern(regexp = "^09\\d{8}$", message = "手機號碼格式不正確")
    private String phone;

    @Pattern(regexp = "ADMIN|MANAGER|WAITER|BARTENDER|KITCHEN", message = "角色不合法")
    private String role;

    // 留空表示不修改密碼
    @Size(min = 8, max = 72)
    private String password;
}
