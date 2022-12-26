package com.pobar.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class CategoryController {
    @GetMapping(value = "login")
    public String loginPage (){
        return "loginPage";
    }

}
