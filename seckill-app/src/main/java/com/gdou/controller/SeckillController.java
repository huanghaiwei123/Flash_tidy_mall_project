package com.gdou.controller;
import com.gdou.common.Result;
import com.gdou.Constant.ResultCodeConstant;
import com.gdou.exception.BusinessException;
import com.gdou.limit.RateLimit;
import com.gdou.pojo.dto.SeckillDto;
import com.gdou.pojo.entity.Seckill;
import com.gdou.service.SeckillService;
import com.gdou.service.UserSeckillRecordService;
import com.gdou.util.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
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
     * 获取验证码（算术题 + 图片 base64）
     */
    @RateLimit
    @GetMapping("/getToken/{seckillId}")
    public Result getToken(@PathVariable Long seckillId) {
        String userId = UserHolder.getUserId();
        log.info("用户{}正在获取验证码进行验证", userId);
        return seckillService.getToken(seckillId, userId);
    }

    /**
     * 获取秒杀商品列表
     */
    @RateLimit
    @GetMapping("/seckills")
    public Result getSeckillList() {
        List<Seckill> list = seckillService.getSeckillList();
        return Result.success(list);
    }

    /**
     * 获取指定商品
     */
    @RateLimit
    @GetMapping("/{id}")
    public Result getSeckillById(@PathVariable("id") Integer id) {
        return Result.success(seckillService.getById(id));
    }

    /**
     * 执行秒杀（需验证码答案）
     */
    @RateLimit
    @PostMapping("/onseckill/{seckillId}")
    public Result onSeckill(@PathVariable Long seckillId, @RequestParam String captchaAnswer) {
        String userId = UserHolder.getUserId();
        log.info("用户{}正在秒杀id为:{}的商品", userId, seckillId);
        return seckillService.onSeckill(seckillId, userId, captchaAnswer);
    }

    /**
     * 新增秒杀活动
     */
    @RateLimit
    @PostMapping
    public Result addSeckill(@RequestBody @Valid SeckillDto seckillDto) {
        return seckillService.addSeckill(seckillDto);
    }

    /**
     * 修改秒杀活动
     */
    @RateLimit
    @PutMapping("/{id}")
    public Result updateSeckill(@PathVariable Long id, @RequestBody @Valid SeckillDto seckillDto) {
        return seckillService.updateSeckill(id, seckillDto);
    }

    /**
     * 删除秒杀活动
     */
    @RateLimit
    @DeleteMapping("/{id}")
    public Result deleteSeckill(@PathVariable Long id) {
        return seckillService.deleteSeckill(id);
    }

    /**
     * 查询秒杀结果
     */
    @RateLimit
    @GetMapping("seckillResult")
    public Result seckillResult() {
        return userSeckillRecordService.seckillResult();
    }
}
