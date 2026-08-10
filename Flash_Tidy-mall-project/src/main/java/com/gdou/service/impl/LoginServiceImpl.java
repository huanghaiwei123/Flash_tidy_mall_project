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
        if (user!=null) {
            String password = user.getPassword();
            Long userId = user.getId();
            boolean verify = PasswordEncrypt.verify(loginDto.getPassword(), password);
            if (verify) {
                Map<String,Object> map = new HashMap<>();
                String jwt = jwtUtil.jwtGenerate(userId, phone);
                map.put("token", jwt);
                // 返回用户信息，前端存入 localStorage 避免每次查库
                map.put("userId", userId);
                map.put("nickname", user.getNickname());
                map.put("phone", user.getPhone());
                map.put("email", user.getEmail());
                map.put("avatar", user.getAvatar());
                map.put("gender", user.getGender());
                return Result.success("用户登录成功", map);
            }else{
                return Result.Fail("用户Token无效，请重试");
            }
        }else{
            log.error("用户未注册，请前往注册");
            return Result.Fail("用户未注册，请前往注册");
        }
    }
}
