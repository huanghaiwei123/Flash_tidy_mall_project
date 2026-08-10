package com.gdou.service;

import com.gdou.common.Result;
import com.gdou.pojo.dto.UserAddressDto;
import com.gdou.pojo.entity.UserAddress;
import com.baomidou.mybatisplus.extension.service.IService;

import javax.validation.Valid;

/**
* @author huanghaiwei
* @description 针对表【user_address(用户收货地址表)】的数据库操作Service
* @createDate 2026-08-06 21:59:46
*/
public interface UserAddressService extends IService<UserAddress> {

    Result insert(Long userId, @Valid UserAddressDto userAddressDto);

    Result query(Long userId);

    Result delete(Long userId, Long id);

    Result update(Long userId,@Valid UserAddressDto userAddressDto);
}
