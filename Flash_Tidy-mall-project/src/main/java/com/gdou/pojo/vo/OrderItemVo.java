package com.gdou.pojo.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class OrderItemVo {
    /** 订单编号 */
    private String orderNo;
    /** 商家ID */
    private Long merchantId;
    /** 店铺名称 */
    private String merchantName;
    /** SPU ID */
    private Long spuId;
    /** SKU 名称快照 */
    private String skuName;
    /** SKU 规格快照 */
    private String skuSpec;
    /** SKU 图片快照 */
    private String skuImage;
    /** SKU 下单时单价快照 */
    private BigDecimal skuPrice;
    /** 购买数量 */
    private Integer quantity;
    /** 明细总价（= sku_price × quantity） */
    private BigDecimal totalPrice;
    /** 创建时间 */
    private Date createTime;
}
