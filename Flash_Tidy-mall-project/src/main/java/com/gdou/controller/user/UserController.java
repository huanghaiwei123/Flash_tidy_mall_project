package com.gdou.controller.user;

import com.gdou.common.Result;
import com.gdou.pojo.dto.MerchantApplyDto;
import com.gdou.pojo.dto.UserDto;
import com.gdou.pojo.dto.UserProfileDto;
import com.gdou.service.MerchantApplicationService;
import com.gdou.service.UserService;
import com.gdou.util.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/hhw/user")
@Slf4j
public class UserController {
    @Autowired
    private UserService userService;
    @Autowired
    private MerchantApplicationService merchantApplicationService;

    /**
     * 修改自己的个人信息
     * @param userDto
     * @return
     */
    @PostMapping("/changePerDetails")
    public Result changePerDetails(@Valid @RequestBody UserProfileDto userDto) {
        Long userId = UserHolder.get();
        log.info("用户{}正在修改自己的个人信息", userId);
        return userService.changePerDetails(userId, userDto);
    }

    @PostMapping("/changePage")
    public Result changeMerchantRolePage() {
        Long userId = UserHolder.get();
        log.info("用户{}想切换页面为卖家中心", userId);
        return userService.changeMerchantRolePage(userId);
    }

    @PostMapping("/changeAdminPage")
    public Result changeAdminRolePage() {
        Long userId = UserHolder.get();
        log.info("用户{}想切换页面为管理后台", userId);
        return userService.changeAdminRolePage(userId);
    }

    /**
     * 提交商家申请
     */
    /**
     * 获取当前用户个人信息
     */
    @GetMapping("/info")
    public Result getUserInfo() {
        Long userId = UserHolder.get();
        return userService.getUserInfo(userId);
    }

    @PostMapping("/applyMerchant")
    public Result applyMerchant(@Valid @RequestBody MerchantApplyDto dto) {
        Long userId = UserHolder.get();
        log.info("用户{}申请成为商家，店铺名: {}", userId, dto.getShopName());
        return merchantApplicationService.apply(userId, dto);
    }
}
