package com.gdou.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gdou.captcha.pojo.CaptchaVO;
import com.gdou.captcha.service.CaptchaService;
import com.gdou.common.Result;
import com.gdou.common.SeckillMetrics;
import com.gdou.constant.MqConstant;
import com.gdou.constant.ResultMsgConstant;
import com.gdou.mapper.SkuMapper;
import com.gdou.mapper.SpuMapper;
import com.gdou.mapper.MerchantMapper;
import com.gdou.mq.MqOrderMessage;
import com.gdou.mq.MqSender;
import com.gdou.pojo.dto.OrderDto;
import com.gdou.pojo.dto.ShoppingCarDto;
import com.gdou.pojo.entity.Sku;
import com.gdou.pojo.entity.Spu;
import com.gdou.pojo.entity.Merchant;
import com.gdou.service.ShoppingCarService;
import com.gdou.service.UserSeckillService;
import io.micrometer.core.instrument.*;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import io.micrometer.core.instrument.distribution.pause.PauseDetector;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.ToDoubleFunction;
import java.util.function.ToLongFunction;
import java.util.stream.Collectors;

@Service
@Slf4j
public class UseSeckillServiceImpl implements UserSeckillService {
    @Autowired
    private SkuMapper skuMapper;
    @Autowired
    private SpuMapper spuMapper;
    @Autowired
    private MerchantMapper merchantMapper;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private DefaultRedisScript<Long> redisScript;
    @Autowired
    private MqSender mqSender;
    @Autowired
    private CaptchaService captchaService;
    @Autowired
    private SeckillMetrics seckillMetrics;

    @Override
    public Result query() {
        Date now = new Date();
        // 查询有效的秒杀 SKU（上架 + 时间范围内）
        LambdaQueryWrapper<Sku> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Sku::getIsSeckill, 1);
        wrapper.le(Sku::getSeckillStartTime, new Date(System.currentTimeMillis()+3*24*60*60*1000));
        wrapper.ge(Sku::getSeckillEndTime, new Date(System.currentTimeMillis()-24*60*60*1000));
        wrapper.eq(Sku::getStatus, 1);

        List<Sku> skus = skuMapper.selectList(wrapper);

        if (skus.isEmpty()) {
            return Result.success("暂无秒杀活动", Collections.emptyList());
        }

        // 批量查 SPU 和商户
        Set<Long> spuIds = skus.stream().map(Sku::getSpuId).collect(Collectors.toSet());
        Map<Long, Spu> spuMap = new HashMap<>();
        for (Long spuId : spuIds) {
            Spu spu = spuMapper.selectById(spuId);
            if (spu != null) spuMap.put(spuId, spu);
        }
        Map<Long, String> shopMap = new HashMap<>();

        // 组装结果
        List<Map<String, Object>> result = new ArrayList<>();
        String stockKey = ResultMsgConstant.REDIS_SECKILL_STOCK_PREFIX;
        for (Sku sku : skus) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("skuId", sku.getId());
            item.put("spuId", sku.getSpuId());
            item.put("skuName", sku.getName());
            item.put("skuSpec", sku.getSpec());
            item.put("skuImage", sku.getImage());
            item.put("originalPrice", sku.getPrice());
            item.put("seckillPrice", sku.getSeckillPrice());
            item.put("seckillStartTime", sku.getSeckillStartTime());
            item.put("seckillEndTime", sku.getSeckillEndTime());
            // Redis 秒杀库存
            String redisStockKey = stockKey + ":" + sku.getId();
            Object redisStock = redisTemplate.opsForValue().get(redisStockKey);
            int stock;
            if (redisStock != null) {
                stock = Integer.parseInt(redisStock.toString());
            } else {
                stock = sku.getPromotionStock() != null ? sku.getPromotionStock() : 0;
            }
            item.put("seckillStock", stock);
            // SPU 信息
            Spu spu = spuMap.get(sku.getSpuId());
            item.put("spuName", spu != null ? spu.getName() : "");
            item.put("spuImage", spu != null ? spu.getMainImage() : "");
            // 店铺名 + merchantId
            if (spu != null && spu.getMerchantId() != null) {
                String shopName = shopMap.get(spu.getMerchantId());
                if (shopName == null) {
                    Merchant merchant = merchantMapper.selectById(spu.getMerchantId());
                    shopName = merchant != null ? merchant.getShopName() : "";
                    shopMap.put(spu.getMerchantId(), shopName);
                }
                item.put("shopName", shopName);
                item.put("merchantId", spu.getMerchantId());
            } else {
                item.put("shopName", "");
                item.put("merchantId", null);
            }
            result.add(item);
        }
        return Result.success(result);
    }

    @Override
    public Result getCaptcha(Long userId) {
        CaptchaVO vo = captchaService.generate();
        String captchaKey = ResultMsgConstant.REDIS_SECKILL_CAPTCHA_PREFIX + ":" + userId;
        // 答案存 Redis，5 分钟有效，下次获取覆盖
        redisTemplate.opsForValue().set(captchaKey, vo.getAnswer(), 5, TimeUnit.MINUTES);
        Map<String, String> data = new HashMap<>();
        data.put("image", vo.getImage());
        data.put("expression", vo.getExpression());
        return Result.success(data);
    }

    @Override
    public Result start(Long userId, OrderDto orderDto,String orderType) {
        // 1. 校验验证码（一次性：校验通过立即删除，防止脚本绕过）
        String captcha = orderDto.getCaptcha();
        if (captcha == null || captcha.trim().isEmpty()) {
            seckillMetrics.recordResult("fail");
            return Result.Fail("请输入验证码");
        }
        String captchaKey = ResultMsgConstant.REDIS_SECKILL_CAPTCHA_PREFIX + ":" + userId;
        Object captchaAnswer = redisTemplate.opsForValue().get(captchaKey);
        if (captchaAnswer == null) {
            seckillMetrics.recordResult("fail");
            return Result.Fail("验证码已过期，请点击图片刷新后重试");
        }
        if (!captchaAnswer.toString().equals(captcha.trim())) {
            seckillMetrics.recordResult("fail");
            return Result.Fail("验证码错误，请重新输入");
        }
        redisTemplate.delete(captchaKey);

        List<ShoppingCarDto> car = orderDto.getCar();
        ShoppingCarDto shoppingCarDto = car.get(0);
        Long skuId = shoppingCarDto.getSkuId();
        // 商家不能秒杀自己店铺的商品
        Sku sku = skuMapper.selectById(skuId);
        if (sku != null) {
            Spu spu = spuMapper.selectById(sku.getSpuId());
            if (spu != null && userId.equals(spu.getMerchantId())) {
                seckillMetrics.recordResult("fail");
                return Result.Fail("不能秒杀自己店铺的商品");
            }
        }
        String stockKey=ResultMsgConstant.REDIS_SECKILL_STOCK_PREFIX+":"+skuId;
        String startTimeKey = ResultMsgConstant.REDIS_SECKILL_STARTTIME_PREFIX + ":" + skuId;
        String endTimeKey = ResultMsgConstant.REDIS_SECKILL_ENDTIME_PREFIX + ":" + skuId;
        String userKey = ResultMsgConstant.REDIS_SECKILL_USER_PREFIX + ":" + userId+":"+skuId;
        Long execute = null;
        try {
            execute = seckillMetrics.recordLuaDuration(() -> {
                return (Long) redisTemplate.execute(redisScript,
                        Arrays.asList(
                                stockKey,
                                startTimeKey,
                                endTimeKey,
                                userKey)
                );
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        if (execute == -1) {
            log.error("用户{}秒杀的商品{}不存在", userId, skuId);
            seckillMetrics.recordResult("fail");
            return Result.Fail("秒杀商品不存在");
        } else if (execute == -2) {
            log.error("用户{}秒杀的商品{}活动未开始", skuId, userId);
            seckillMetrics.recordResult("fail");
            return Result.Fail("秒杀活动未开始");
        } else if (execute == -3) {
            log.error("用户{}秒杀的商品{}活动已结束", skuId, userId);
            seckillMetrics.recordResult("fail");
            return Result.Fail("秒杀活动已结束");
        }else if (execute == -4) {
            log.error("用户{}已经参与过商品{}的秒杀活动", skuId, userId);
            seckillMetrics.recordResult("repeat");
            return Result.Fail("该用户已经参与过这类商品的秒杀活动");
        } else if (execute == -5) {
            log.error("商品{}库存不足",skuId);
            seckillMetrics.recordResult("sold_out");
            return Result.Fail("商品{}库存不足，无法秒杀", skuId);
        }
        seckillMetrics.recordResult("success");
        MqOrderMessage message = MqOrderMessage.builder()
                .messageId(UUID.randomUUID().toString())
                .orderType(orderType)
                .orderDto(orderDto)
                .userId(userId)
                .build();
        mqSender.orderSend(MqConstant.MQ_ORDER_EXCHANGE,MqConstant.MQ_ORDER_ROUTING_KEY,message);
        return Result.success("秒杀成功");
    }
}
