package com.pobar.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pobar.dto.user.UserCreateRequest;
import com.pobar.dto.user.UserResponse;
import com.pobar.dto.user.UserUpdateRequest;
import com.pobar.entity.User;
import com.pobar.exception.BusinessException;
import com.pobar.logging.Audit;
import com.pobar.mapper.UserMapper;
import com.pobar.service.UserManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserManagementServiceImpl implements UserManagementService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public List<UserResponse> listAll() {
        return userMapper.selectList(new LambdaQueryWrapper<User>().orderByAsc(User::getId))
                .stream().map(this::toResponse).toList();
    }

    @Override
    @Audit(action = "CREATE_USER", entityType = "User")
    public UserResponse create(UserCreateRequest request) {
        if (userMapper.findByAccount(request.getAccount()) != null) {
            throw new BusinessException(409, "帳號已存在");
        }
        User user = new User();
        user.setAccount(request.getAccount());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setRole(request.getRole());
        user.setIsActive(1);
        userMapper.insert(user);
        return toResponse(user);
    }

    @Override
    @Audit(action = "UPDATE_USER", entityType = "User")
    public UserResponse update(Integer id, UserUpdateRequest request) {
        User user = userMapper.selectById(id);
        if (user == null) throw new BusinessException(404, "找不到此用戶");

        if (request.getEmail() != null) user.setEmail(request.getEmail());
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        if (request.getRole() != null) user.setRole(request.getRole());
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        userMapper.updateById(user);
        return toResponse(user);
    }

    @Override
    @Audit(action = "DEACTIVATE_USER", entityType = "User")
    public void deactivate(Integer id) {
        User user = userMapper.selectById(id);
        if (user == null) throw new BusinessException(404, "找不到此用戶");
        user.setIsActive(0);
        userMapper.updateById(user);
    }

    private UserResponse toResponse(User u) {
        UserResponse r = new UserResponse();
        r.setId(u.getId());
        r.setAccount(u.getAccount());
        r.setEmail(u.getEmail());
        r.setPhone(u.getPhone());
        r.setRole(u.getRole());
        r.setIsActive(u.getIsActive());
        r.setCreatedAt(u.getCreatedAt());
        return r;
    }
}
