package com.gdou.controller.user;

import com.gdou.common.Result;
import com.gdou.pojo.dto.OrderDto;
import com.gdou.service.UserSeckillService;
import com.gdou.util.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/hhw/seckill")
@Slf4j
public class UserSeckillController {
    @Autowired
    private UserSeckillService userSeckillService;

    /**
     * 用户查询秒杀商品
     * @return
     */
    @GetMapping("/query")
    public Result query() {
        Long userId = UserHolder.get();
        log.info("用户{}正在查看秒杀商品", userId);
        return  userSeckillService.query();
    }

    /**
     * 获取秒杀验证码（算术题图片）
     * @return
     */
    @GetMapping("/captcha")
    public Result captcha() {
        Long userId = UserHolder.get();
        log.info("用户{}正在获取秒杀验证码", userId);
        return userSeckillService.getCaptcha(userId);
    }

    /**
     * 用户点击秒杀活动，秒杀活动为一人一单
     * @param orderDto
     * @return
     */
    @PostMapping("/start")
    public Result start(@RequestBody OrderDto orderDto) {
        Long userId = UserHolder.get();
        log.info("用户{}正在秒杀id为{}的商品", userId, orderDto);
        String orderType="SECKILL";
        return userSeckillService.start(userId, orderDto,orderType);
    }
}
