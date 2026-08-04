package com.gdou.task;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gdou.Constant.RedisConstant;
import com.gdou.common.Result;
import com.gdou.config.SeckillBloomFilter;
import com.gdou.pojo.entity.Seckill;
import com.gdou.service.SeckillService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
public class SeckillTask {
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private SeckillService seckillService;
    @Autowired
    private SeckillBloomFilter bloomFilter;

    //    每12小时检查一次是否有要添加的秒杀活动，活动提前一天上线，顺便解决redis宕机导致服务器缓存丢失的问题
    @Scheduled(cron = "0 0 */12 * * ?")
    public void autoWarmStock() {
        LambdaQueryWrapper<Seckill> wrapper = new LambdaQueryWrapper<>();
        wrapper.le(Seckill::getStartTime, LocalDateTime.now().plusDays(1L));
        List<Seckill> list = seckillService.list(wrapper);
        for (Seckill seckill : list) {
            int integer = seckill.getNumber() / RedisConstant.BUCKET_COUNT;
            int remainder = seckill.getNumber() % RedisConstant.BUCKET_COUNT;
            String key = RedisConstant.SECKILL_STOCK + ":{" + seckill.getSeckillId() + "}";
            boolean anyNew = false;
            for (int i = 0; i < RedisConstant.BUCKET_COUNT; i++) {
                int addNum = i < remainder ? 1 : 0;   //余数加给前几个
                // 库存分桶
                Boolean b = redisTemplate.opsForValue().setIfAbsent(key + ":" + i, integer + addNum, Duration.ofDays(2L));
                if (Boolean.TRUE.equals(b)) {
                    anyNew = true;
                }
            }
            // 缓存时间窗口到 Redis（epoch 毫秒），Lua 脚本内部校验
            String startKey = RedisConstant.SECKILL_START + ":{" + seckill.getSeckillId() + "}";
            String endKey = RedisConstant.SECKILL_END + ":{" + seckill.getSeckillId() + "}";
            long startEpoch = seckill.getStartTime().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
            long endEpoch = seckill.getEndTime().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
            redisTemplate.opsForValue().setIfAbsent(startKey, startEpoch, Duration.ofDays(2L));
            redisTemplate.opsForValue().setIfAbsent(endKey, endEpoch, Duration.ofDays(2L));
            bloomFilter.add(seckill.getSeckillId());
            if (anyNew) {
                log.info("活动开始前一天已上架id为{}的商品,过期时间为一天", seckill.getSeckillId());
            }
        }
    }

    @PostConstruct
    public void initBloomFilter() {
        List<Seckill> all = seckillService.list();
        for (Seckill s : all) {
            bloomFilter.add(s.getSeckillId());
        }
        log.info("布隆过滤器初始加载完成，共 {} 条", all.size());
    }

}
