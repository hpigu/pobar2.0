package com.pobar.controller;

import com.pobar.common.Result;
import com.pobar.dto.user.UserCreateRequest;
import com.pobar.dto.user.UserResponse;
import com.pobar.dto.user.UserUpdateRequest;
import com.pobar.service.UserManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class UserManagementController {

    private final UserManagementService userManagementService;

    @GetMapping
    public Result<List<UserResponse>> list() {
        return Result.ok(userManagementService.listAll());
    }

    @PostMapping
    public Result<UserResponse> create(@Valid @RequestBody UserCreateRequest request) {
        return Result.ok(userManagementService.create(request));
    }

    @PutMapping("/{id}")
    public Result<UserResponse> update(@PathVariable Integer id,
                                       @Valid @RequestBody UserUpdateRequest request) {
        return Result.ok(userManagementService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<?> deactivate(@PathVariable Integer id) {
        userManagementService.deactivate(id);
        return Result.ok();
    }
}
