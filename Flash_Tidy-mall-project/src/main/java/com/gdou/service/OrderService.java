package com.gdou.service;

import com.gdou.common.Result;
import com.gdou.pojo.dto.OrderDto;
import com.gdou.pojo.entity.Order;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author huanghaiwei
* @description 针对表【order(统一订单表)】的数据库操作Service
* @createDate 2026-08-06 21:59:46
*/
public interface OrderService extends IService<Order> {

    Result orderQueryByUser(Long userId,String orderNo);

    Result orderQueryByMerchant(Long userId,String orderNo);

    Result orderQueryListByUser(Long userId);

    /**
     * 查询用户订单（可选状态过滤）
     * @param userId 用户ID
     * @param status 订单状态（PENDING_PAY/PAID/SHIPPED/RECEIVED/COMPLETED/CANCELLED/REFUNDING/REFUNDED），可空表示不限制
     */
    Result orderQueryListByUser(Long userId, String status);

    /**
     * 查询用户订单（可选状态 + 商品关键词过滤）
     * @param userId 用户ID
     * @param status 订单状态，可空
     * @param keyword 商品关键词（匹配订单明细的 skuName/skuSpec），只返回包含此关键词的订单；可空表示不限制
     */
    Result orderQueryListByUser(Long userId, String status, String keyword);

    Result orderQueryListByMerchant(Long merchantId);

    Result orderCancel(Long userId, String orderNo);

    Result orderCreate(Long userId,OrderDto orderDto,String orderType);

    /**
     * 支付成功后更新订单状态
     * @param orderNo 订单编号
     * @param tradeNo 支付宝交易号
     */
    void paySuccess(String orderNo, String tradeNo);

    /**
     * 商家发货
     * @param merchantId 商家ID
     * @param orderNo 订单编号
     * @return 发货结果
     */
    Result ship(Long merchantId, String orderNo);

    /**
     * 用户确认收货
     * @param userId  用户ID
     * @param orderNo 订单编号
     * @return 收货结果
     */
    Result receive(Long userId, String orderNo);

    /**
     * 恢复订单锁定库存（取消/退款时调用）
     * @param orderNo 订单编号
     */
    void restoreStock(String orderNo);

    /**
     * 再来一单 — 将订单商品重新加入购物车
     * @param userId  用户ID
     * @param orderNo 订单编号
     * @return 结果
     */
    Result reorder(Long userId, String orderNo);

}
