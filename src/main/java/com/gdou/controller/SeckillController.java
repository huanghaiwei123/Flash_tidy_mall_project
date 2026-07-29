package com.gdou.controller;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gdou.common.Result;
import com.gdou.limit.RateLimit;
import com.gdou.pojo.entity.Seckill;
import com.gdou.pojo.entity.UserSeckillRecord;
import com.gdou.service.SeckillService;
import com.gdou.service.UserSeckillRecordService;
import com.gdou.util.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@Slf4j
@RequestMapping("/hhw/seckill")
public class SeckillController {
    @Autowired
    private SeckillService seckillService;
    @Autowired
    private UserSeckillRecordService userSeckillRecordService;
    /**
     * 获取秒杀商品列表
     * @return
     */
    @RateLimit
    @GetMapping("/seckills")
    public Result getSeckillList() {
        List<Seckill> list = seckillService.getSeckillList();
        return Result.success(list);
    }

    /**
     * 获取指定商品
     * @param id
     * @return
     */
    @RateLimit
    @GetMapping("/{id}")
    public Result getSeckillById(@PathVariable("id") Integer id) {
        return Result.success(seckillService.getById(id));
    }

    /**
     * 开启秒杀
     * @param seckillId
     * @return
     */
    @RateLimit
    @PostMapping("/onseckill/{seckillId}")
    public Result onSeckill(@PathVariable Long seckillId) {
        String userId = UserHolder.getUserId();
        log.info("用户；{}正在秒杀id为:{}的商品",userId,seckillId);
        return seckillService.onSeckill(seckillId,userId);
    }

    /**
     * 管理员预热接口
     */
    @RateLimit
    @PostMapping("/warmstock/{seckillId}")
    public Result warmStock(@PathVariable Long seckillId) {
        return seckillService.warmStock(seckillId);
    }

    /**
     * 查询秒杀结果
     * @param seckillId
     * @return
     */
    @RateLimit
    @GetMapping("seckillResult/{seckillId}")
    public Result seckillResult(@PathVariable Long seckillId) {
        return userSeckillRecordService.seckillResult(seckillId);
    }
}
