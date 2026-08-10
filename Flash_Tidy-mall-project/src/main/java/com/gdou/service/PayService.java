package com.gdou.service;

import com.gdou.common.Result;

import java.util.Map;

/**
 * 支付服务接口
 */
public interface PayService {

    /**
     * 发起支付宝页面支付
     * @param userId  用户ID
     * @param orderNo 订单编号
     * @return 包含支付宝支付表单 HTML 的 Result
     */
    Result pay(Long userId, String orderNo);

    /**
     * 支付宝异步通知处理
     * @param params 支付宝 POST 过来的参数 Map
     * @return "success" 或 "failure"（支付宝协议要求纯文本）
     */
    String handleNotify(Map<String, String> params);

    /**
     * 退款
     * @param userId  用户ID
     * @param orderNo 订单编号
     * @return 退款结果
     */
    Result refund(Long userId, String orderNo);

    /**
     * 主动查询支付宝支付结果（同步回调备用）
     * @param orderNo 订单编号
     */
    void queryPayResult(String orderNo);
}
