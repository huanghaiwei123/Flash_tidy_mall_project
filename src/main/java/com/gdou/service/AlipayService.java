package com.gdou.service;

import com.gdou.common.Result;
import com.gdou.pojo.entity.SeckillOrder;

import java.util.Map;

public interface AlipayService {
    /**
     * 生成支付宝电脑网站HTML支付页面
     * @param seckillOrder
     * @return
     */
    public String generatePayPage(SeckillOrder seckillOrder);

    /**
     * 验证支付宝异步回调签名
     * @param params
     * @return
     */
    public boolean verifyNotify(Map<String,String> params);

    /**
     * 执行退款
     * @param seckillOrder
     * @return
     */
    public String executeRefund(SeckillOrder seckillOrder);
}
