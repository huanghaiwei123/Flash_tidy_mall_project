package com.gdou.controller;

import com.gdou.common.Result;
import com.gdou.pojo.dto.RegisterDto;
import com.gdou.service.RegisterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

@RestController
@RequestMapping("hhw")
public class RegisterController {
    @Autowired
    private RegisterService registerService;
    @PostMapping("/register")
    public Result Register(@Valid @RequestBody RegisterDto registerDto) {
        return registerService.register(registerDto);
    }
}
