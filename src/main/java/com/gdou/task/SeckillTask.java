package com.gdou.task;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gdou.Constant.RedisConstant;
import com.gdou.common.Result;
import com.gdou.pojo.entity.Seckill;
import com.gdou.service.SeckillService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
public class SeckillTask {
    @Autowired
    private RedisTemplate<String,Object> redisTemplate;
    @Autowired
    private SeckillService seckillService;
//    每三分钟检查一次是否有要添加的秒杀活动，顺便解决redis宕机导致服务器缓存丢失的问题
    @Scheduled(cron = "0 */3 * * * ?")
    public void autoWarmStock(){
        LambdaQueryWrapper<Seckill> wrapper = new LambdaQueryWrapper<>();
        wrapper.le(Seckill::getStartTime, LocalDateTime.now().plusMinutes(5L));
        List<Seckill> list = seckillService.list(wrapper);
        for(Seckill seckill : list){
            String key=RedisConstant.SECKILL_STOCK+seckill.getSeckillId();
            redisTemplate.opsForValue().setIfAbsent(key,seckill.getNumber(),Duration.ofDays(1L));
            Duration duration = Duration.between(seckill.getStartTime(),LocalDateTime.now());
            log.info("活动开始前{}分钟已上架id为{}的商品,过期时间为一天",duration.toMinutes(),seckill.getSeckillId());
        }
    }
}
