package com.gdou.controller;
import com.gdou.Constant.RedisConstant;
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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

@RestController
@Slf4j
@RequestMapping("/hhw/seckill")
public class SeckillController {
    @Autowired
    private SeckillService seckillService;
    @Autowired
    private UserSeckillRecordService userSeckillRecordService;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 获取秒杀 token（动态 URL 防脚本）
     * @param seckillId
     * @return 5 秒有效的随机 token
     */
    @RateLimit
    @GetMapping("/getToken/{seckillId}")
    public Result getSeckillToken(@PathVariable Long seckillId) {
        String token = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String key = RedisConstant.SECKILL_TOKEN + ":{" + seckillId + "}:" + token;
        redisTemplate.opsForValue().set(key, "1", Duration.ofSeconds(5));
        log.info("下发秒杀 token：seckillId={}, token={}", seckillId, token);
        return Result.success(token);
    }
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
     * 开启秒杀（需动态 token）
     * @param seckillId
     * @param token  秒杀令牌，先调用 getToken 获取
     * @return
     */
    @RateLimit
    @PostMapping("/onseckill/{seckillId}")
    public Result onSeckill(@PathVariable Long seckillId, @RequestParam String token) {
        // ① 校验动态 token（防脚本提前知道 URL）
        String tokenKey = RedisConstant.SECKILL_TOKEN + ":{" + seckillId + "}:" + token;
        Boolean exists = redisTemplate.hasKey(tokenKey);
        if (exists == null || !exists) {
            throw new BusinessException("秒杀URL已失效，请重新获取", ResultCodeConstant.ERROR);
        }
        // ② 验证通过，立即删除（一次性 token）
        redisTemplate.delete(tokenKey);
        String userId = UserHolder.getUserId();
        log.info("用户；{}正在秒杀id为:{}的商品",userId,seckillId);
        return seckillService.onSeckill(seckillId,userId);
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
     * @return
     */
    @RateLimit
    @GetMapping("seckillResult")
    public Result seckillResult() {
        return userSeckillRecordService.seckillResult();
    }
}
