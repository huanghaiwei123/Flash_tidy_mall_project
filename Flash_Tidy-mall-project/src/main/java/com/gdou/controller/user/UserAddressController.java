package com.gdou.controller.user;

import com.gdou.common.Result;
import com.gdou.pojo.dto.UserAddressDto;
import com.gdou.pojo.entity.User;
import com.gdou.pojo.entity.UserAddress;
import com.gdou.service.UserAddressService;
import com.gdou.util.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/hhw/address")
@Slf4j
public class UserAddressController {
    @Autowired
    private UserAddressService userAddressService;

    /**
     * 用户添加收货地址
     * @param userAddressDto
     * @return
     */
    @PostMapping("/insert")
    public Result insertUserAddress(@Valid @RequestBody UserAddressDto userAddressDto) {
        Long userId = UserHolder.get();
        log.info("用户{}正在新增收货地址", userId);
        return userAddressService.insert(userId,userAddressDto);
    }

    /**
     * 用户查看收货地址
     * @return
     */
    @GetMapping("/query")
    public Result queryUserAddress() {
        Long userId = UserHolder.get();
        log.info("用户{}正在查看购物车", userId);
        return userAddressService.query(userId);
    }

    /**
     * 用户删除收货地址
     * @param id
     * @return
     */
    @PostMapping("/delete/{id}")
    public Result deleteUserAddress(@PathVariable Long id) {
        Long userId = UserHolder.get();
        log.info("用户{}正在删除地址{}", userId, id);
        return userAddressService.delete(userId,id);
    }

    /**
     * 用户更改收货地址
     * @param addressId
     * @param userAddressDto
     * @return
     */
    @PostMapping("/update/{addressId}")
    public Result updateUserAddress(@PathVariable Long addressId,@Valid @RequestBody UserAddressDto userAddressDto) {
        Long userId = UserHolder.get();
        log.info("用户{}正在修改收货地址{}", userId, addressId);
        userAddressDto.setId(addressId);
        return userAddressService.update(userId,userAddressDto);
    }
}
