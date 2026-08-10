package com.gdou.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gdou.common.Result;
import com.gdou.pojo.entity.ShoppingCar;

/**
 * 购物车 Service
 */
public interface ShoppingCarService extends IService<ShoppingCar> {

    /**
     * 添加商品到购物车（已存在则增加数量）
     */
    Result addToCart(Long userId, Long spuId, Long skuId, Integer quantity);

    /**
     * 更新购物车商品数量
     */
    Result updateQuantity(Long userId, Long cartId, Integer quantity);

    /**
     * 更新勾选状态
     */
    Result updateSelected(Long userId, Long cartId, Integer selected);

    /**
     * 全选/取消全选
     */
    Result selectAll(Long userId, Integer selected);

    /**
     * 删除购物车商品
     */
    Result removeFromCart(Long userId, Long cartId);

    /**
     * 查看购物车列表
     */
    Result queryCart(Long userId);

    /**
     * 查询购物车总价和总数量
     */
    Result cartSummary(Long userId);
}
