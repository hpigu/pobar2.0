package com.pobar.controller;

import com.pobar.model.LoginForm;
import com.pobar.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class UserController {
    @Autowired
    UserService userService;

    @PostMapping(value = "/login")
    public String login(Model model, LoginForm loginForm) {
        boolean isLogin = userService.checkUserAccount(loginForm.getAccount(), loginForm.getPassword());
        if (isLogin) {
            return "index";
        } else {
            model.addAttribute("errorMsg","帳號或密碼錯誤");
            return "loginPage";
        }
    }
}
