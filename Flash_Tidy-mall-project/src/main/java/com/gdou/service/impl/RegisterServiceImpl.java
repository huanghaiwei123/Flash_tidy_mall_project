package com.gdou.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gdou.common.Result;
import com.gdou.mapper.UserMapper;
import com.gdou.pojo.dto.RegisterDto;
import com.gdou.pojo.entity.User;
import com.gdou.service.RegisterService;
import com.gdou.util.PasswordEncrypt;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.UUID;

@Service
@Slf4j
public class RegisterServiceImpl implements RegisterService {
    @Autowired
    private UserMapper userMapper;

    /**
     * 注册
     * @param registerDto
     * @return
     */
    @Override
    @Transactional
    public Result register(RegisterDto registerDto) {
        String password = registerDto.getPassword();
        String phone = registerDto.getPhone();
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getPhone, phone);
        Long l = userMapper.selectCount(wrapper);
        if (l > 0) {
            log.info("该用户已注册");
            return Result.Fail("该用户已注册");
        }else{
            User user = new User();
            user.setPhone(phone);
            user.setPassword(PasswordEncrypt.encrypt(password));
            user.setUsername("该用户暂未设置昵称"+ UUID.randomUUID());
            user.setCreateTime(new Date());
            user.setUpdateTime(new Date());
            userMapper.insert(user);
        }
        log.info("注册成功，用户：{}", registerDto);
        return Result.success("注册成功");
    }
}
