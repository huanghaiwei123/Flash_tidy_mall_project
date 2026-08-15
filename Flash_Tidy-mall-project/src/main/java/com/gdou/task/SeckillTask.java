package com.gdou.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gdou.constant.ResultMsgConstant;
import com.gdou.mapper.SkuMapper;
import com.gdou.pojo.entity.Sku;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class SeckillTask {
    @Autowired
    private SkuMapper skuMapper;
    @Autowired
    private RedisTemplate<String,Object> redisTemplate;

    /**
     * 秒杀活动redis提前一天预热上线
     */
    @PostConstruct  //启动时执行预热
    @Scheduled(fixedRate = 12*60*60*1000)  //项目启动后每隔12小时执行一次
    public void addSeckillStock(){
        MDC.put("traceId", "task-seckill-" + System.currentTimeMillis());
        try {
            String stockPrefix = ResultMsgConstant.REDIS_SECKILL_STOCK_PREFIX;
            String starttimePrefix = ResultMsgConstant.REDIS_SECKILL_STARTTIME_PREFIX;
            String endtimePrefix = ResultMsgConstant.REDIS_SECKILL_ENDTIME_PREFIX;
            LambdaQueryWrapper<Sku> skuLambdaQueryWrapper = new LambdaQueryWrapper<>();
            skuLambdaQueryWrapper.eq(Sku::getStatus, 1);
            skuLambdaQueryWrapper.eq(Sku::getIsSeckill, 1);
            // 开始时间在未来24小时内，且结束时间未过
            skuLambdaQueryWrapper.lt(Sku::getSeckillStartTime, new Date(System.currentTimeMillis() + 86400000L));
            skuLambdaQueryWrapper.gt(Sku::getSeckillEndTime, new Date());
            List<Sku> skus = skuMapper.selectList(skuLambdaQueryWrapper);
            String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            if (skus.isEmpty()) {
                log.info("当前时间为{}，目前还没有要提前预热的秒杀商品", time);
                return;
            }
            for (Sku sku : skus) {
                String stockKey = stockPrefix + ":" + sku.getId();
                String startTimeKey = starttimePrefix + ":" + sku.getId();
                String endTimeKey = endtimePrefix + ":" + sku.getId();
                // 使用 promotionStock（秒杀配额），而非总库存
                int seckillStock = sku.getPromotionStock() != null ? sku.getPromotionStock() : 0;
                redisTemplate.opsForValue().setIfAbsent(stockKey, seckillStock, 2L, TimeUnit.DAYS);
                redisTemplate.opsForValue().setIfAbsent(startTimeKey, sku.getSeckillStartTime().getTime() / 1000, 2L, TimeUnit.DAYS);
                redisTemplate.opsForValue().setIfAbsent(endTimeKey, sku.getSeckillEndTime().getTime() / 1000, 2L, TimeUnit.DAYS);
                log.info("秒杀预热 SKU={}，促销库存={}，时间 {} ~ {}", sku.getId(), seckillStock, sku.getSeckillStartTime(), sku.getSeckillEndTime());
            }
        }finally {
            MDC.clear();
        }
    }

}
