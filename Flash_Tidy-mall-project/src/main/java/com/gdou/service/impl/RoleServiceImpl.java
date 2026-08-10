package com.gdou.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gdou.pojo.entity.Role;
import com.gdou.service.RoleService;
import com.gdou.mapper.RoleMapper;
import org.springframework.stereotype.Service;

/**
* @author huanghaiwei
* @description 针对表【role(角色表)】的数据库操作Service实现
* @createDate 2026-08-06 21:59:46
*/
@Service
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role>
    implements RoleService{

}




