package com.gdou.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gdou.common.Result;
import com.gdou.mapper.RoleMapper;
import com.gdou.mapper.UserRoleMapper;
import com.gdou.pojo.dto.UserDto;
import com.gdou.pojo.entity.Role;
import com.gdou.pojo.entity.User;
import com.gdou.pojo.entity.UserRole;
import com.gdou.service.UserService;
import com.gdou.mapper.UserMapper;
import com.gdou.util.PasswordEncrypt;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
* @author huanghaiwei
* @description 针对表【user(用户表)】的数据库操作Service实现
* @createDate 2026-08-06 21:59:46
*/
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService{
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private UserRoleMapper userRoleMapper;
    @Autowired
    private RoleMapper roleMapper;
    @Override
    @Transactional
    public Result changePerDetails(Long userId,UserDto userDto) {
       User user = new User();
       BeanUtils.copyProperties(userDto, user);
//       修改密码对密码进行加密
       if(userDto.getPassword()!=null && !userDto.getPassword().isEmpty()){
           user.setPassword(PasswordEncrypt.encrypt(userDto.getPassword()));
       }
       if(userDto.getNickname()!=null && !userDto.getNickname().isEmpty()){

       }
       if(userDto.getNickname()!=null && !userDto.getNickname().isEmpty()){
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getNickname,userDto.getNickname());
        Long count = userMapper.selectCount(queryWrapper);
        if(count>0) {
            log.info("{}这个昵称已存在", userDto.getNickname());
            return Result.Fail("该昵称已存在，请重试：{}",userDto.getNickname());
        }
       }
       LambdaUpdateWrapper<User> wrapper = new LambdaUpdateWrapper<>();
       wrapper.eq(User::getId, userId);
       userMapper.update(user, wrapper);
       return Result.success("用户信息修改成功,{}",userDto);
    }

    @Override
    public Result changeMerchantRolePage(Long userId) {
        // 1. 查 user_role 表，按 userId 找该用户的角色关联
        LambdaQueryWrapper<UserRole> urWrapper = new LambdaQueryWrapper<>();
        urWrapper.eq(UserRole::getUserId, userId);
        UserRole userRole = userRoleMapper.selectOne(urWrapper);

        if (userRole == null) {
            log.info("用户{}不是商家", userId);
            return Result.Fail("该用户还不是商家，请前往注册");
        }

        // 2. 查 role 表获取角色编码
        Role role = roleMapper.selectById(userRole.getRoleId());
        if (role == null || !"MERCHANT".equals(role.getCode())) {
            log.info("用户{}不是商家", userId);
            return Result.Fail("该用户还不是商家，请前往注册");
        }

        return Result.success("用户已经注册商家", userId);
    }

    @Override
    public Result changeAdminRolePage(Long userId) {
        LambdaQueryWrapper<UserRole> urWrapper = new LambdaQueryWrapper<>();
        urWrapper.eq(UserRole::getUserId, userId);
        UserRole userRole = userRoleMapper.selectOne(urWrapper);

        if (userRole == null) {
            return Result.Fail("该用户不是管理员");
        }

        Role role = roleMapper.selectById(userRole.getRoleId());
        if (role == null || !"ADMIN".equals(role.getCode())) {
            return Result.Fail("该用户不是管理员");
        }

        return Result.success("用户是管理员", userId);
    }

    @Override
    public Result getUserInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.Fail("用户不存在");
        }
        // 不返回密码
        user.setPassword(null);
        return Result.success(user);
    }
}




