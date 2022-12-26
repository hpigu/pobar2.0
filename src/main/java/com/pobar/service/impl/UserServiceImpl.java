package com.pobar.service.impl;

import com.pobar.dao.UserRepository;
import com.pobar.entity.UserEntity;
import com.pobar.service.UserService;
import com.pobar.util.SHA1SaltUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserRepository userRepository;

    @Override
    public boolean checkUserAccount(String account, String password) {
        UserEntity user = userRepository.getUserByAccount(account);
        if (user != null) {
            String hashPassword = SHA1SaltUtil.hash(password, user.getSalt());
            return user.getPassword().equals(hashPassword);
        }
        return false;
    }
}
