package com.gdou.agent;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Agent 订单卡片数据
 */
@Data
public class AgentOrderCard {
    private String orderNo;
    private String status;
    private BigDecimal totalAmount;
    private String createTime;
    private List<OrderItem> items = new ArrayList<>();

    @Data
    public static class OrderItem {
        private String skuName;
        private String merchantName;
        private Integer quantity;
        private BigDecimal skuPrice;
    }
}
