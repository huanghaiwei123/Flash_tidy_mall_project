package com.gdou.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gdou.common.Result;
import com.gdou.mapper.MerchantMapper;
import com.gdou.mapper.SkuMapper;
import com.gdou.mapper.SpuMapper;
import com.gdou.pojo.entity.Merchant;
import com.gdou.pojo.entity.Sku;
import com.gdou.pojo.entity.Spu;
import com.gdou.pojo.vo.SpuListVo;
import com.gdou.service.MerchantService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class MerchantServiceImpl extends ServiceImpl<MerchantMapper, Merchant>
        implements MerchantService {

    @Autowired
    private MerchantMapper merchantMapper;
    @Autowired
    private SpuMapper spuMapper;
    @Autowired
    private SkuMapper skuMapper;

    @Override
    public Result shopDetail(Long merchantId, Integer page, Integer size) {
        if (page == null || page < 1) page = 1;
        if (size == null || size < 1) size = 20;

        // 查店铺信息
        Merchant merchant = merchantMapper.selectById(merchantId);
        if (merchant == null || merchant.getStatus() == 0) {
            return Result.Fail("店铺不存在或已关闭");
        }

        // 查该店铺上架商品，分页
        LambdaQueryWrapper<Spu> spuWrapper = new LambdaQueryWrapper<>();
        spuWrapper.eq(Spu::getMerchantId, merchantId)
                .eq(Spu::getStatus, 1)
                .orderByDesc(Spu::getSales)
                .orderByDesc(Spu::getCreateTime);
        Page<Spu> spuPage = new Page<>(page, size);
        spuPage = spuMapper.selectPage(spuPage, spuWrapper);

        // 组装 VO
        List<SpuListVo> voList = spuPage.getRecords().stream().map(spu -> {
            SpuListVo vo = new SpuListVo();
            BeanUtils.copyProperties(spu, vo);
            LambdaQueryWrapper<Sku> skuWrapper = new LambdaQueryWrapper<>();
            skuWrapper.eq(Sku::getSpuId, spu.getId())
                    .eq(Sku::getStatus, 1)
                    .orderByAsc(Sku::getPrice)
                    .last("limit 1");
            Sku cheapest = skuMapper.selectOne(skuWrapper);
            vo.setMinPrice(cheapest != null ? cheapest.getPrice() : BigDecimal.ZERO);
            LambdaQueryWrapper<Sku> countWrapper = new LambdaQueryWrapper<>();
            countWrapper.eq(Sku::getSpuId, spu.getId()).eq(Sku::getStatus, 1);
            vo.setSkuCount(Integer.valueOf(String.valueOf(skuMapper.selectCount(countWrapper))));
            return vo;
        }).collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("shop", merchant);
        result.put("total", spuPage.getTotal());
        result.put("page", page);
        result.put("size", size);
        result.put("records", voList);

        log.info("用户浏览店铺：merchantId={}, page={}, 共{}件商品", merchantId, page, spuPage.getTotal());
        return Result.success(result);
    }
}
