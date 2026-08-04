package com.gdou.controller;

import com.gdou.common.Result;
import com.gdou.pojo.dto.LoginDto;
import com.gdou.pojo.entity.SeckillUser;
import com.gdou.service.LoginService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/hhw")
public class LoginController {
    @Autowired
    private LoginService loginService;

    @PostMapping("/login")
    public Result login(@RequestBody @Valid LoginDto loginDto, HttpServletRequest request) {
        SeckillUser user = new SeckillUser();
        BeanUtils.copyProperties(loginDto, user);
        String ip = getClientIp(request);
        log.info("用户登录:{}，IP:{}", user.getPhone(), ip);
        return loginService.login(user, ip);
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
