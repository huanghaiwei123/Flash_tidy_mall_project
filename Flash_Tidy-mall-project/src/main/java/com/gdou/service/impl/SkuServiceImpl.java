package com.gdou.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gdou.common.Result;
import com.gdou.constant.ResultMsgConstant;
import com.gdou.exception.BusinessException;
import com.gdou.mapper.SkuMapper;
import com.gdou.mapper.SpuMapper;
import com.gdou.pojo.dto.SkuDto;
import com.gdou.pojo.entity.Sku;
import com.gdou.pojo.entity.Spu;
import com.gdou.service.SkuService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
* @author huanghaiwei
* @description 针对表【sku(SKU 库存单元表（含秒杀 + 库存三字段）)】的数据库操作Service实现
* @createDate 2026-08-06 21:59:46
*/
@Service
@Slf4j
public class SkuServiceImpl extends ServiceImpl<SkuMapper, Sku>
    implements SkuService{

    @Autowired
    private SkuMapper skuMapper;

    @Autowired
    private SpuMapper spuMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 校验 SPU 是否属于当前商家，若不属则抛异常
     */
    private Spu verifySpuOwnership(Long merchantId, Long spuId) {
        Spu spu = spuMapper.selectById(spuId);
        if (spu == null) {
            throw new BusinessException("SPU 不存在");
        }
        if (!spu.getMerchantId().equals(merchantId)) {
            throw new BusinessException("无权操作该商品下的 SKU");
        }
        return spu;
    }

    /**
     * 校验 SKU 是否属于当前商家（通过 SPU 归属链）
     */
    private Sku verifySkuOwnership(Long merchantId, Long skuId) {
        Sku sku = skuMapper.selectById(skuId);
        if (sku == null) {
            throw new BusinessException("SKU 不存在");
        }
        // 通过 SPU 校验归属
        verifySpuOwnership(merchantId, sku.getSpuId());
        return sku;
    }

    @Override
    @Transactional
    public Result createSku(Long merchantId, SkuDto skuDto) {
        // 校验 SPU 归属
        verifySpuOwnership(merchantId, skuDto.getSpuId());

        Sku sku = new Sku();
        BeanUtils.copyProperties(skuDto, sku);

        // 初始化系统管理字段
        sku.setLockedStock(0);
        // availableStock = stock（初始时 lockedStock = 0）
        Integer stock = skuDto.getStock() != null ? skuDto.getStock() : 0;
        sku.setAvailableStock(stock);

        // 促销库存默认值
        if (sku.getPromotionStock() == null) {
            sku.setPromotionStock(0);
        }
        // 秒杀默认值
        if (sku.getIsSeckill() == null) {
            sku.setIsSeckill(0);
        }
        // 状态默认上架
        if (sku.getStatus() == null) {
            sku.setStatus(1);
        }
        // 排序默认值
        if (sku.getSort() == null) {
            sku.setSort(0);
        }

        sku.setCreateTime(new Date());
        sku.setUpdateTime(new Date());
        save(sku);

        log.info("商家 {} 创建 SKU：{}（SPU={}, 库存={}）", merchantId, sku.getName(), sku.getSpuId(), sku.getStock());
        return Result.success("SKU 创建成功", sku);
    }

    @Override
    @Transactional
    public Result updateSku(Long merchantId, Long skuId, SkuDto skuDto) {
        Sku sku = verifySkuOwnership(merchantId, skuId);

        // 如果修改了 SPU，需要校验新 SPU 归属
        if (skuDto.getSpuId() != null && !skuDto.getSpuId().equals(sku.getSpuId())) {
            verifySpuOwnership(merchantId, skuDto.getSpuId());
        }

        // 记录旧 stock 值，用于计算 availableStock 变化
        Integer oldStock = sku.getStock();

        BeanUtils.copyProperties(skuDto, sku);

        // 如果 stock 被修改，重新计算 availableStock
        if (skuDto.getStock() != null && !skuDto.getStock().equals(oldStock)) {
            int stockDiff = skuDto.getStock() - oldStock;
            sku.setAvailableStock(sku.getAvailableStock() + stockDiff);
            log.info("商家 {} 修改 SKU {} 库存：{} → {}，可售库存调整为 {}",
                    merchantId, skuId, oldStock, skuDto.getStock(), sku.getAvailableStock());
        }

        sku.setUpdateTime(new Date());
        updateById(sku);

        log.info("商家 {} 更新 SKU：{}（id={}）", merchantId, sku.getName(), skuId);
        return Result.success("SKU 更新成功", sku);
    }

    @Override
    public Result querySkuBySpu(Long merchantId, Long spuId) {
        // 校验 SPU 归属
        verifySpuOwnership(merchantId, spuId);

        LambdaQueryWrapper<Sku> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Sku::getSpuId, spuId)
               .orderByAsc(Sku::getSort)
               .orderByDesc(Sku::getCreateTime);
        List<Sku> list = skuMapper.selectList(wrapper);

        log.info("商家 {} 查询 SPU {} 下的 SKU 列表，共 {} 条", merchantId, spuId, list.size());
        return Result.success(list);
    }

    @Override
    public Result getSkuDetail(Long merchantId, Long skuId) {
        Sku sku = verifySkuOwnership(merchantId, skuId);
        log.info("商家 {} 查看 SKU 详情：{}", merchantId, skuId);
        return Result.success(sku);
    }

    @Override
    @Transactional
    public Result deleteSku(Long merchantId, Long skuId) {
        Sku sku = verifySkuOwnership(merchantId, skuId);
        skuMapper.deleteById(skuId);
        log.info("商家 {} 删除 SKU：{}（{}）", merchantId, skuId, sku.getName());
        return Result.success("SKU 删除成功");
    }

    @Override
    @Transactional
    public Result updateSkuStatus(Long merchantId, Long skuId, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            return Result.Fail("状态值只能为 0（下架）或 1（上架）");
        }
        Sku sku = verifySkuOwnership(merchantId, skuId);
        sku.setStatus(status);
        sku.setUpdateTime(new Date());
        updateById(sku);

        String statusText = status == 1 ? "上架" : "下架";
        log.info("商家 {} 将 SKU {} {} ", merchantId, skuId, statusText);
        return Result.success("SKU 已" + statusText);
    }

    @Override
    public Result queryAllMySkus(Long merchantId) {
        // 查该商家所有 SPU
        LambdaQueryWrapper<Spu> spuWrapper = new LambdaQueryWrapper<>();
        spuWrapper.eq(Spu::getMerchantId, merchantId);
        List<Spu> spuList = spuMapper.selectList(spuWrapper);

        if (spuList.isEmpty()) {
            log.info("商家 {} 暂无商品", merchantId);
            return Result.success("暂无商品", null);
        }

        // 收集所有 SPU ID
        List<Long> spuIds = spuList.stream()
                .map(Spu::getId)
                .collect(Collectors.toList());

        // 查所有关联 SKU
        LambdaQueryWrapper<Sku> skuWrapper = new LambdaQueryWrapper<>();
        skuWrapper.in(Sku::getSpuId, spuIds)
                  .orderByAsc(Sku::getSpuId)
                  .orderByAsc(Sku::getSort)
                  .orderByDesc(Sku::getCreateTime);
        List<Sku> skuList = skuMapper.selectList(skuWrapper);

        log.info("商家 {} 查询所有 SKU，共 {} 条", merchantId, skuList.size());
        return Result.success(skuList);
    }

    @Override
    public Result userQuerySkusBySpu(Long spuId) {
        LambdaQueryWrapper<Sku> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Sku::getSpuId, spuId)
               .eq(Sku::getStatus, 1)  // 只返回上架的
               .orderByAsc(Sku::getSort)
               .orderByAsc(Sku::getPrice);
        List<Sku> list = skuMapper.selectList(wrapper);
        return Result.success(list);
    }

    @Override
    public Result queryMySeckillSkus(Long merchantId) {
        // 1. 查该商家所有 SPU
        LambdaQueryWrapper<Spu> spuWrapper = new LambdaQueryWrapper<>();
        spuWrapper.eq(Spu::getMerchantId, merchantId);
        List<Spu> spuList = spuMapper.selectList(spuWrapper);

        if (spuList.isEmpty()) {
            return Result.success(Collections.emptyList());
        }

        List<Long> spuIds = spuList.stream().map(Spu::getId).collect(Collectors.toList());
        Map<Long, Spu> spuMap = spuList.stream().collect(Collectors.toMap(Spu::getId, s -> s));

        // 2. 查这些 SPU 下所有参与秒杀的 SKU
        LambdaQueryWrapper<Sku> skuWrapper = new LambdaQueryWrapper<>();
        skuWrapper.in(Sku::getSpuId, spuIds)
                  .eq(Sku::getIsSeckill, 1)
                  .orderByAsc(Sku::getSeckillStartTime);
        List<Sku> skuList = skuMapper.selectList(skuWrapper);

        // 3. 组装结果，附加 Redis 库存和 SPU 名称
        Date now = new Date();
        String stockPrefix = ResultMsgConstant.REDIS_SECKILL_STOCK_PREFIX;
        List<Map<String, Object>> result = new ArrayList<>();

        for (Sku sku : skuList) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("skuId", sku.getId());
            item.put("spuId", sku.getSpuId());
            item.put("skuName", sku.getName());
            item.put("skuPrice", sku.getPrice());
            item.put("seckillPrice", sku.getSeckillPrice());
            item.put("promotionStock", sku.getPromotionStock());
            item.put("seckillStartTime", sku.getSeckillStartTime());
            item.put("seckillEndTime", sku.getSeckillEndTime());
            item.put("status", sku.getStatus());

            // SPU 名称
            Spu spu = spuMap.get(sku.getSpuId());
            item.put("spuName", spu != null ? spu.getName() : "");

            // 秒杀状态：未开始 / 进行中 / 已结束
            if (sku.getSeckillStartTime() != null && now.before(sku.getSeckillStartTime())) {
                item.put("seckillStatus", "PENDING");
                item.put("seckillStatusText", "未开始");
            } else if (sku.getSeckillEndTime() != null && now.after(sku.getSeckillEndTime())) {
                item.put("seckillStatus", "ENDED");
                item.put("seckillStatusText", "已结束");
            } else {
                item.put("seckillStatus", "ACTIVE");
                item.put("seckillStatusText", "进行中");
            }

            // Redis 实时库存
            String redisKey = stockPrefix + ":" + sku.getId();
            Object redisStock = redisTemplate.opsForValue().get(redisKey);
            if (redisStock != null) {
                item.put("redisStock", Integer.parseInt(redisStock.toString()));
            } else {
                item.put("redisStock", sku.getPromotionStock() != null ? sku.getPromotionStock() : 0);
            }

            result.add(item);
        }
        return Result.success(result);
    }
}
