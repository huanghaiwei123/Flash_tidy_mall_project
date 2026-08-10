package com.gdou.pojo.vo;

import java.math.BigDecimal;
import java.util.Date;

public class OrderItemVo {
    /**
     * 订单编号（冗余，方便查询）
     */
    private String orderNo;
    /**
     * SKU 名称快照
     */
    private String skuName;

    /**
     * SKU 规格快照
     */
    private String skuSpec;

    /**
     * SKU 图片快照
     */
    private String skuImage;

    /**
     * SKU 下单时单价快照
     */
    private BigDecimal skuPrice;

    /**
     * 购买数量
     */
    private Integer quantity;

    /**
     * 明细总价（= sku_price × quantity）
     */
    private BigDecimal totalPrice;

    /**
     * 创建时间
     */
    private Date createTime;
}
