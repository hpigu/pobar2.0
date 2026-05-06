package com.pobar.service;

import com.pobar.dto.user.UserCreateRequest;
import com.pobar.dto.user.UserResponse;
import com.pobar.dto.user.UserUpdateRequest;

import java.util.List;

public interface UserManagementService {

    List<UserResponse> listAll();

    UserResponse create(UserCreateRequest request);

    UserResponse update(Integer id, UserUpdateRequest request);

    void deactivate(Integer id);
}
