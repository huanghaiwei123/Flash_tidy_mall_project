package com.gdou.controller;

import com.gdou.common.Result;
import com.gdou.pojo.dto.LoginDto;
import com.gdou.service.LoginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/hhw")
public class LoginController {
    @Autowired
    private LoginService loginService;
    @PostMapping("/login")
    public Result login(@Valid @RequestBody LoginDto loginDto) {
        return loginService.login(loginDto);
    }
}
