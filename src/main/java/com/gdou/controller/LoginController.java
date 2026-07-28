package com.gdou.controller;

import com.gdou.common.Result;
import com.gdou.pojo.dto.LoginDto;
import com.gdou.pojo.entity.SeckillUser;
import com.gdou.service.LoginService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/hhw")
public class LoginController {
    @Autowired
    private LoginService loginService;
    @PostMapping("/login")
    public Result login(@RequestBody @Valid LoginDto loginDto) {
        SeckillUser user = new SeckillUser();
        BeanUtils.copyProperties(loginDto, user);
        log.info("用户登录:{}", user);
        return loginService.login(user);
    }

}
