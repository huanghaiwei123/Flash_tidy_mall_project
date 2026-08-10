package com.gdou.service;

import com.gdou.common.Result;
import com.gdou.pojo.dto.UserDto;
import com.gdou.pojo.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author huanghaiwei
* @description 针对表【user(用户表)】的数据库操作Service
* @createDate 2026-08-06 21:59:46
*/
public interface UserService extends IService<User> {
    Result changePerDetails(Long userId,UserDto userDto) ;

    Result changeMerchantRolePage(Long userId);

    Result changeAdminRolePage(Long userId);

    Result getUserInfo(Long userId);
}
