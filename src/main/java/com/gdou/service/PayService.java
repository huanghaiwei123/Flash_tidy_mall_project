package com.gdou.service;

import com.gdou.common.Result;

import java.util.Map;

public interface PayService {
    /**
     * 发起支付宝支付
     * @param orderId
     * @return
     */
    String pay(Long orderId);

    /**
     * 取消支付，回补库存
     * @param orderId
     * @return
     */
    Result cancel(Long orderId);

    /**
     * 退款
     * @param orderId
     * @return
     */
    Result refund(Long orderId);

    /**
     * 处理支付宝异步回调
     */
    String handleAlipayNotify(Map<String, String> params);
}
