package com.gdou.pojo.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class OrderVo {
    /**
     * 订单编号（唯一，业务流水号）
     */
    private String orderNo;

    /**
     * 订单状态：PENDING_PAY=待支付 PAID=已支付 SHIPPED=已发货 RECEIVED=已收货 COMPLETED=已完成 CANCELLED=已取消 REFUNDING=退款中 REFUNDED=已退款
     */
    private String status;

    /**
     * 商品总金额
     */
    private BigDecimal totalAmount;

    /**
     * 实付金额
     */
    private BigDecimal payAmount;

    /**
     * 收货地址快照（下单时冗余存储，不受地址变更影响）
     */
    private Object addressSnapshot;

    /**
     * 支付时间
     */
    private Date payTime;

    /**
     * 支付方式：ALIPAY=支付宝
     */
    private String payType;

    /**
     * 支付宝交易号
     */
    private String tradeNo;

    /**
     * 取消时间
     */
    private Date cancelTime;

    /**
     * 创建时间（下单时间）
     */
    private Date createTime;

}
