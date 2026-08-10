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

    Result orderQueryListByMerchant(Long merchantId);

    Result orderCancel(Long userId, String orderNo);

    Result orderCreate(Long userId,OrderDto orderDto);

    /**
     * 支付成功后更新订单状态
     * @param orderNo 订单编号
     * @param tradeNo 支付宝交易号
     */
    void paySuccess(String orderNo, String tradeNo);

}
