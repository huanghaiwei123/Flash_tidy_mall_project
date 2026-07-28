package com.gdou.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gdou.Constant.ResultCodeConstant;
import com.gdou.common.Result;
import com.gdou.exception.BusinessException;
import com.gdou.mapper.SeckillUserMapper;
import com.gdou.pojo.entity.SeckillUser;
import com.gdou.service.RegisterService;
import com.gdou.util.PasswordEncoder;
import lombok.extern.slf4j.Slf4j;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class RegisterServiceImpl implements RegisterService {
    @Autowired
    private SeckillUserMapper seckillUserMapper;
    @Override
    public Result register(SeckillUser seckillUser) {
//        判断是否手机号是否已被注册，若是已被注册就抛出业务异常被全局异常捕获，返回结果码给前端，但程序不会中断
        LambdaQueryWrapper<SeckillUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SeckillUser::getPhone, seckillUser.getPhone());
        Long count = seckillUserMapper.selectCount(wrapper);
        if (count > 0) {
            throw new BusinessException("手机号已被注册", ResultCodeConstant.ERROR);
        }
       wrapper.eq(SeckillUser::getNickname, seckillUser.getNickname());
        Long count1 = seckillUserMapper.selectCount(wrapper);
        if (count1 > 0) {
            log.error("该昵称已存在");
            throw new BusinessException("该昵称已存在", ResultCodeConstant.ERROR);
        }
//        手机号未被注册则继续
//        这里使用Bcrypt工具类对密码进行加密
        String encode = PasswordEncoder.encode(seckillUser.getPassword());
        seckillUser.setPassword(encode);
//        插入数据
        seckillUserMapper.insert(seckillUser);
        return Result.success();
    }
}
