package com.gdou.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gdou.cache.DoubleCacheService;
import com.gdou.common.Result;
import com.gdou.exception.BusinessException;
import com.gdou.mapper.CategoryMapper;
import com.gdou.mapper.SkuMapper;
import com.gdou.mapper.SpuMapper;
import com.gdou.pojo.dto.SpuDto;
import com.gdou.pojo.entity.Category;
import com.gdou.pojo.entity.Sku;
import com.gdou.pojo.entity.Spu;
import com.gdou.pojo.vo.SpuDetailVo;
import com.gdou.pojo.vo.SpuListVo;
import com.gdou.service.SpuSearchService;
import com.gdou.service.SpuService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
* @author huanghaiwei
* @description 针对表【spu(商品 SPU 主表)】的数据库操作Service实现
* @createDate 2026-08-06 21:59:46
*/
@Service
@Slf4j
public class SpuServiceImpl extends ServiceImpl<SpuMapper, Spu>
    implements SpuService{

    @Autowired
    private SpuMapper spuMapper;
    @Autowired
    private CategoryMapper categoryMapper;
    @Autowired
    private SkuMapper skuMapper;
    @Autowired
    private SpuSearchService spuSearchService;
    @Autowired
    private DoubleCacheService doubleCacheService;
    @Autowired
    private ObjectMapper objectMapper;

    // ===================== 商家端 =====================

    @Override
    public Result spuQuery(Long merchantId) {
        LambdaQueryWrapper<Spu> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Spu::getMerchantId, merchantId)
               .orderByDesc(Spu::getCreateTime);
        List<Spu> list = spuMapper.selectList(wrapper);
        if(list.isEmpty()){
            log.info("商家{}的店铺暂时没有商品", merchantId);
        }
        return Result.success(list);
    }

    @Override
    @Transactional
    public Result spuSave(Long merchantId, SpuDto spuDto) {
        Spu spu = new Spu();
        BeanUtils.copyProperties(spuDto, spu);
        Long categoryId = spuDto.getCategoryId();
        if (categoryId != null) {
            // 优先使用下拉框选中的分类ID
            Category category = categoryMapper.selectById(categoryId);
            if (category == null) return Result.Fail("分类不存在");
            spu.setCategoryName(category.getName());
        } else if (spuDto.getCategoryName() != null && !spuDto.getCategoryName().isEmpty()) {
            // 兼容手动输入分类名
            LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Category::getName, spuDto.getCategoryName());
            Category category = categoryMapper.selectOne(wrapper);
            if (category == null) return Result.Fail("分类不存在，请先创建分类");
            categoryId = category.getId();
        } else {
            return Result.Fail("商品分类不能为空");
        }
        spu.setMerchantId(merchantId);
        spu.setCategoryId(categoryId);
        spu.setCreateTime(new Date());
        spu.setUpdateTime(new Date());
        spu.setSales(0);
        if (spu.getStatus() == null) spu.setStatus(1);
        save(spu);
        log.info("商家{}上架商品：{}", merchantId, spu.getName());
        return Result.success(spu);
    }

    @Override
    @Transactional
    public Result spuUpdate(Long merchantId, Long spuId, SpuDto spuDto) {
        Spu spu = spuMapper.selectById(spuId);
        if (spu == null) {
            return Result.Fail("商品不存在");
        }
        if (!spu.getMerchantId().equals(merchantId)) {
            throw new BusinessException("无权操作该商品");
        }
        BeanUtils.copyProperties(spuDto, spu);
        spu.setId(spuId);
        // 分类更新：categoryId 优先，categoryName 兜底
        if (spuDto.getCategoryId() != null) {
            Category category = categoryMapper.selectById(spuDto.getCategoryId());
            if (category != null) {
                spu.setCategoryId(category.getId());
                spu.setCategoryName(category.getName());
            }
        } else if (spuDto.getCategoryName() != null && !spuDto.getCategoryName().isEmpty()) {
            LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Category::getName, spuDto.getCategoryName());
            Category category = categoryMapper.selectOne(wrapper);
            if (category != null) {
                spu.setCategoryId(category.getId());
            }
        }
        spu.setUpdateTime(new Date());
        updateById(spu);
        // 清缓存，防止用户读到旧商品信息
        doubleCacheService.evict("spu:" + spuId);
        log.info("商家{}更新商品：{}", merchantId, spu.getName());
        return Result.success("商品更新成功", spu);
    }

    @Override
    @Transactional
    public Result spuUpdateStatus(Long merchantId, Long spuId, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            return Result.Fail("状态值只能为 0（下架）或 1（上架）");
        }
        Spu spu = spuMapper.selectById(spuId);
        if (spu == null) {
            return Result.Fail("商品不存在");
        }
        if (!spu.getMerchantId().equals(merchantId)) {
            throw new BusinessException("无权操作该商品");
        }
        spu.setStatus(status);
        spu.setUpdateTime(new Date());
        updateById(spu);
        // 下架/上架也要清缓存，防止用户读到旧状态
        doubleCacheService.evict("spu:" + spuId);
        String statusText = status == 1 ? "上架" : "下架";
        log.info("商家{}将商品{} {}", merchantId, spuId, statusText);
        return Result.success("商品已" + statusText);
    }

    // ===================== 用户端 =====================

    @Override
    public Result userSpuList(Long categoryId, String keyword, Integer page, Integer size) {
        if (page == null || page < 1) page = 1;
        if (size == null || size < 1) size = 20;

        // 构建分类 ID 集合（含子分类）
        Set<Long> categoryIds = new HashSet<>();
        if (categoryId != null) {
            categoryIds.add(categoryId);
            // 找子分类
            LambdaQueryWrapper<Category> childWrapper = new LambdaQueryWrapper<>();
            childWrapper.eq(Category::getParentId, categoryId);
            List<Category> children = categoryMapper.selectList(childWrapper);
            for (Category child : children) {
                categoryIds.add(child.getId());
                // 找孙分类
                LambdaQueryWrapper<Category> grandchildWrapper = new LambdaQueryWrapper<>();
                grandchildWrapper.eq(Category::getParentId, child.getId());
                List<Category> grandchildren = categoryMapper.selectList(grandchildWrapper);
                grandchildren.forEach(gc -> categoryIds.add(gc.getId()));
            }
        }

        // 优先走 ES，异常时回退 MySQL LIKE
        try {
            return spuSearchService.search(categoryIds, keyword, page, size);
        } catch (Exception e) {
            log.warn("ES 搜索异常，回退 MySQL LIKE: {}", e.getMessage());
            return mysqlSearch(categoryIds, keyword, page, size);
        }
    }

    /** 原 MySQL LIKE 搜索（ES 挂掉时的兜底） */
    private Result mysqlSearch(Set<Long> categoryIds, String keyword, Integer page, Integer size) {
        LambdaQueryWrapper<Spu> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Spu::getStatus, 1); // 只查上架商品
        if (!categoryIds.isEmpty()) {
            wrapper.in(Spu::getCategoryId, categoryIds);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Spu::getName, keyword).or().like(Spu::getBrand, keyword));
        }
        wrapper.orderByDesc(Spu::getSales).orderByDesc(Spu::getCreateTime);

        Page<Spu> spuPage = new Page<>(page, size);
        spuPage = spuMapper.selectPage(spuPage, wrapper);

        // 组装 VO（含最低价）
        List<SpuListVo> voList = spuPage.getRecords().stream().map(spu -> {
            SpuListVo vo = new SpuListVo();
            BeanUtils.copyProperties(spu, vo);
            // 查该 SPU 下最低上架 SKU 售价
            LambdaQueryWrapper<Sku> skuWrapper = new LambdaQueryWrapper<>();
            skuWrapper.eq(Sku::getSpuId, spu.getId())
                     .eq(Sku::getStatus, 1)
                     .orderByAsc(Sku::getPrice)
                     .last("limit 1");
            Sku cheapest = skuMapper.selectOne(skuWrapper);
            vo.setMinPrice(cheapest != null ? cheapest.getPrice() : BigDecimal.ZERO);
            // SKU 数量
            LambdaQueryWrapper<Sku> countWrapper = new LambdaQueryWrapper<>();
            countWrapper.eq(Sku::getSpuId, spu.getId()).eq(Sku::getStatus, 1);
            vo.setSkuCount(Integer.valueOf(String.valueOf(skuMapper.selectCount(countWrapper))));
            return vo;
        }).collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("total", spuPage.getTotal());
        result.put("page", page);
        result.put("size", size);
        result.put("records", voList);

        log.info("用户浏览商品列表（MySQL 兜底）：categoryIds={}, keyword={}, page={}, 共{}条",
                categoryIds, keyword, page, spuPage.getTotal());
        return Result.success(result);
    }

    @Override
    public Result userSpuDetail(Long spuId) {
        String key="spu:"+spuId;
        String result = doubleCacheService.get(key, () -> {
            Spu spu = spuMapper.selectById(spuId);
            if (spu == null || spu.getStatus() == 0) {
                log.info("id为{}商品信息不存在", spuId);
                return null;
            }
            SpuDetailVo vo = new SpuDetailVo();
            BeanUtils.copyProperties(spu, vo);
            // 查上架 SKU
            LambdaQueryWrapper<Sku> skuWrapper = new LambdaQueryWrapper<>();
            skuWrapper.eq(Sku::getSpuId, spuId)
                    .eq(Sku::getStatus, 1)
                    .orderByAsc(Sku::getSort)
                    .orderByAsc(Sku::getPrice);
            List<Sku> skuList = skuMapper.selectList(skuWrapper);
            vo.setSkuList(skuList);
            try {
                return objectMapper.writeValueAsString(vo);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        });
        try {
            log.info("用户查看商品详情：spuId={}", spuId);
            return Result.success(objectMapper.readValue(result, SpuDetailVo.class));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
