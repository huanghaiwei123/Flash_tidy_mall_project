package com.gdou.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gdou.common.Result;
import com.gdou.mapper.ShoppingCarMapper;
import com.gdou.mapper.SkuMapper;
import com.gdou.pojo.entity.ShoppingCar;
import com.gdou.pojo.entity.Sku;
import com.gdou.service.ShoppingCarService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 购物车 Service 实现
 */
@Service
@Slf4j
public class ShoppingCarServiceImpl extends ServiceImpl<ShoppingCarMapper, ShoppingCar>
        implements ShoppingCarService {

    @Autowired
    private ShoppingCarMapper shoppingCarMapper;
    @Autowired
    private SkuMapper skuMapper;

    @Override
    @Transactional
    public Result addToCart(Long userId, Long spuId, Long skuId, Integer quantity) {
        // 检查 SKU 是否存在且上架
        Sku sku = skuMapper.selectById(skuId);
        if (sku == null || sku.getStatus() == 0) {
            return Result.Fail("商品已下架或不存在");
        }
        // 检查购物车是否已有该 SKU
        LambdaQueryWrapper<ShoppingCar> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShoppingCar::getUserId, userId)
               .eq(ShoppingCar::getSkuId, skuId);
        ShoppingCar car = shoppingCarMapper.selectOne(wrapper);
        if (car != null) {
            // 已存在，累加数量
            car.setQuantity(car.getQuantity() + quantity);
            car.setUpdateTime(new Date());
            shoppingCarMapper.updateById(car);
            log.info("用户 {} 购物车 SKU {} 数量+{} → {}", userId, skuId, quantity, car.getQuantity());
        } else {
            car = new ShoppingCar();
            car.setUserId(userId);
            car.setSpuId(spuId);
            car.setSkuId(skuId);
            car.setQuantity(quantity);
            car.setSelected(1);
            car.setCreateTime(new Date());
            car.setUpdateTime(new Date());
            shoppingCarMapper.insert(car);
            log.info("用户 {} 添加 SKU {} 到购物车 ×{}", userId, skuId, quantity);
        }
        return Result.success("已加入购物车", car);
    }

    @Override
    @Transactional
    public Result updateQuantity(Long userId, Long cartId, Integer quantity) {
        ShoppingCar car = shoppingCarMapper.selectById(cartId);
        if (car == null || !car.getUserId().equals(userId)) {
            return Result.Fail("购物车记录不存在");
        }
        if (quantity <= 0) {
            // 数量 ≤ 0 直接删除
            shoppingCarMapper.deleteById(cartId);
            return Result.success("已移出购物车");
        }
        car.setQuantity(quantity);
        car.setUpdateTime(new Date());
        shoppingCarMapper.updateById(car);
        return Result.success("数量已更新", car);
    }

    @Override
    @Transactional
    public Result updateSelected(Long userId, Long cartId, Integer selected) {
        ShoppingCar car = shoppingCarMapper.selectById(cartId);
        if (car == null || !car.getUserId().equals(userId)) {
            return Result.Fail("购物车记录不存在");
        }
        car.setSelected(selected);
        shoppingCarMapper.updateById(car);
        return Result.success(selected == 1 ? "已勾选" : "已取消勾选");
    }

    @Override
    @Transactional
    public Result selectAll(Long userId, Integer selected) {
        LambdaUpdateWrapper<ShoppingCar> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ShoppingCar::getUserId, userId)
               .set(ShoppingCar::getSelected, selected);
        update(null, wrapper);
        return Result.success(selected == 1 ? "已全选" : "已取消全选");
    }

    @Override
    @Transactional
    public Result removeFromCart(Long userId, Long cartId) {
        ShoppingCar car = shoppingCarMapper.selectById(cartId);
        if (car == null || !car.getUserId().equals(userId)) {
            return Result.Fail("购物车记录不存在");
        }
        shoppingCarMapper.deleteById(cartId);
        log.info("用户 {} 删除购物车记录 {}", userId, cartId);
        return Result.success("已移出购物车");
    }

    @Override
    public Result queryCart(Long userId) {
        LambdaQueryWrapper<ShoppingCar> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShoppingCar::getUserId, userId)
               .orderByDesc(ShoppingCar::getCreateTime);
        List<ShoppingCar> list = shoppingCarMapper.selectList(wrapper);

        // 补充 SKU 详情（名称、价格、图片）
        List<Map<String, Object>> result = list.stream().map(car -> {
            Sku sku = skuMapper.selectById(car.getSkuId());
            Map<String, Object> item = new HashMap<>();
            item.put("cartId", car.getId());
            item.put("spuId", car.getSpuId());
            item.put("skuId", car.getSkuId());
            item.put("quantity", car.getQuantity());
            item.put("selected", car.getSelected());
            item.put("createTime", car.getCreateTime());
            if (sku != null) {
                item.put("skuName", sku.getName());
                item.put("skuPrice", sku.getPrice());
                item.put("skuImage", sku.getImage());
                item.put("skuSpec", sku.getSpec());
                // 小计
                item.put("subtotal", sku.getPrice().multiply(
                        BigDecimal.valueOf(car.getQuantity())));
            }
            return item;
        }).collect(java.util.stream.Collectors.toList());

        return Result.success(result);
    }

    @Override
    public Result cartSummary(Long userId) {
        LambdaQueryWrapper<ShoppingCar> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShoppingCar::getUserId, userId)
               .eq(ShoppingCar::getSelected, 1);  // 只算勾选的
        List<ShoppingCar> selectedItems = shoppingCarMapper.selectList(wrapper);

        int totalCount = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (ShoppingCar car : selectedItems) {
            Sku sku = skuMapper.selectById(car.getSkuId());
            if (sku != null && sku.getStatus() == 1) {
                totalCount += car.getQuantity();
                totalAmount = totalAmount.add(
                        sku.getPrice().multiply(BigDecimal.valueOf(car.getQuantity())));
            }
        }

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalCount", totalCount);
        summary.put("totalAmount", totalAmount);
        summary.put("itemCount", selectedItems.size());
        return Result.success(summary);
    }
}
