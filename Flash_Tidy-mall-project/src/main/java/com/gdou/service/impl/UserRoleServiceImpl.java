package com.gdou.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gdou.pojo.entity.UserRole;
import com.gdou.service.UserRoleService;
import com.gdou.mapper.UserRoleMapper;
import org.springframework.stereotype.Service;

/**
* @author huanghaiwei
* @description 针对表【user_role(用户角色关联表（多对多）)】的数据库操作Service实现
* @createDate 2026-08-06 21:59:46
*/
@Service
public class UserRoleServiceImpl extends ServiceImpl<UserRoleMapper, UserRole>
    implements UserRoleService{

}




