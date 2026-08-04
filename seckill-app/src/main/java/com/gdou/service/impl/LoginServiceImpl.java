package com.gdou.service.impl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gdou.Constant.ResultCodeConstant;
import com.gdou.common.Result;
import com.gdou.exception.BusinessException;
import com.gdou.mapper.SeckillUserMapper;
import com.gdou.pojo.entity.SeckillUser;
import com.gdou.service.LoginService;
import com.gdou.util.JwtUtil;
import com.gdou.util.PasswordEncoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.HashMap;

@Service
@Slf4j
public class LoginServiceImpl implements LoginService {
    @Autowired
    private SeckillUserMapper seckillUserMapper;
    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public Result login(SeckillUser seckillUser, String ip) {
        LambdaQueryWrapper<SeckillUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SeckillUser::getPhone, seckillUser.getPhone());
        Long count = seckillUserMapper.selectCount(wrapper);
        if(count==0){
            log.error("手机号为：{}的用户还未注册",seckillUser.getPhone());
            throw new BusinessException("该用户的手机号还未注册", ResultCodeConstant.ERROR);
        }
        SeckillUser user = seckillUserMapper.selectOne(wrapper);
        if(PasswordEncoder.check(seckillUser.getPassword(), user.getPassword())){
            log.info("登陆成功");
            user.setLastLoginTime(LocalDateTime.now());
            seckillUserMapper.updateById(user);
            String token = jwtUtil.generateToken(user.getId().toString(), user.getPhone(), ip);
            HashMap<String, String> map = new HashMap<>();
            map.put("token", token);
            map.put("nickname", user.getNickname());
            return Result.success(map);
        }else{
            return Result.error("密码不正确，请重新输入",ResultCodeConstant.ERROR,null);
        }
    }
}
