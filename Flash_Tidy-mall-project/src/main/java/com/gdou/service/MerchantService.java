package com.gdou.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gdou.common.Result;
import com.gdou.pojo.entity.Merchant;

/**
 * 商家店铺服务
 */
public interface MerchantService extends IService<Merchant> {

    /**
     * 用户端 — 查看店铺信息（含商品列表）
     */
    Result shopDetail(Long merchantId, Integer page, Integer size);
}
