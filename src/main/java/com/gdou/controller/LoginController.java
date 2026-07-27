package com.gdou.controller;

import com.gdou.common.Result;
import com.gdou.service.LoginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/hhw")
public class LoginController {
    @Autowired
    private LoginService loginService;

    @GetMapping("/login")
    public Result login() {
        loginService.login();
        return Result.success();
    }
    @GetMapping("/register")
    public Result register() {
        return Result.success();
    }
}
