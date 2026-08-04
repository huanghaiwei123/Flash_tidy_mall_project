package com.gdou.controller;
import com.gdou.common.Result;
import com.gdou.pojo.dto.RegisterDto;
import com.gdou.pojo.entity.SeckillUser;
import com.gdou.service.RegisterService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@Slf4j
@RequestMapping("/hhw")
public class RegisterController {
    @Autowired
    private RegisterService registerService;
    @PostMapping("/register")
    public Result register(@RequestBody @Valid RegisterDto registerDto) {
        log.info("用户注册:{}", registerDto);
        SeckillUser user = new SeckillUser();
        BeanUtils.copyProperties(registerDto, user);
        return registerService.register(user);
    }
}
