package com.pobar.controller;

import com.pobar.model.LoginForm;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class UserController {
    @PostMapping(value = "/login")
    public String login(Model model, LoginForm loginForm) {
        return null;
    }
}
