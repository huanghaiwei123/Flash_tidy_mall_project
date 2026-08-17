package com.gdou.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gdou.common.Result;
import com.gdou.mapper.UserMapper;
import com.gdou.pojo.dto.LoginDto;
import com.gdou.pojo.entity.User;
import com.gdou.service.LoginService;
import com.gdou.util.JwtUtil;
import com.gdou.util.PasswordEncrypt;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class LoginServiceImpl implements LoginService {
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private JwtUtil jwtUtil;
    @Override
    public Result login(LoginDto loginDto) {
        String phone = loginDto.getPhone();
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getPhone, phone);
        User user = userMapper.selectOne(wrapper);
        // 统一提示"手机号或密码错误"，避免暴露账号是否存在（防枚举）
        if (user != null && PasswordEncrypt.verify(loginDto.getPassword(), user.getPassword())) {
            Map<String,Object> map = new HashMap<>();
            String jwt = jwtUtil.jwtGenerate(user.getId(), phone);
            map.put("token", jwt);
            // 返回用户信息，前端存入 localStorage 避免每次查库
            map.put("userId", user.getId());
            map.put("nickname", user.getNickname());
            map.put("phone", user.getPhone());
            map.put("email", user.getEmail());
            map.put("avatar", user.getAvatar());
            map.put("gender", user.getGender());
            return Result.success("用户登录成功", map);
        }
        log.warn("用户{}登录失败：手机号或密码错误", phone);
        return Result.Fail("手机号或密码错误");
    }
}
