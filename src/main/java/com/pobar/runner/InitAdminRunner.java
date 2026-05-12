package com.pobar.runner;

import com.pobar.entity.User;
import com.pobar.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InitAdminRunner implements ApplicationRunner {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.init-admin.account:admin}")
    private String adminAccount;

    @Value("${app.init-admin.password:#{null}}")
    private String adminPassword;

    @Override
    public void run(ApplicationArguments args) {
        if (adminPassword == null || adminPassword.isBlank()) {
            return; // 未設定初始密碼則跳過，避免每次重啟都建立帳號
        }

        User existing = userMapper.findByAccount(adminAccount);
        if (existing != null) {
            return; // 已存在則不重複建立
        }

        User admin = new User();
        admin.setAccount(adminAccount);
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setRole("ADMIN");
        admin.setIsActive(true);
        // 首次登入強制改密碼；改完後旗標自動清掉
        admin.setMustChangePassword(true);
        userMapper.insert(admin);
        log.info("已初始化管理員帳號: {}（首次登入會被強制改密碼，請儘速登入並從 .env 移除 INIT_ADMIN_PASSWORD）", adminAccount);
    }
}
