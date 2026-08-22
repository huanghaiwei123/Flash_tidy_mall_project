package com.gdou.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gdou.common.Result;
import com.gdou.service.OrderService;
import com.gdou.service.SpuSearchService;
import com.gdou.util.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Agent 工具注册中心：把商城能力封装成模型可调用的工具
 */
@Slf4j
@Configuration
public class Tools {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SpuSearchService spuSearchService;

    @Autowired
    private AgentContext agentContext;

    @Autowired
    private OrderService orderService;

    /**
     * 工具一：搜索商品（公开能力，无需登录）
     */
    @Bean
    public Tool searchGoodsTool() {
        String schema = "{\"type\":\"object\",\"properties\":{" +
                "\"keyword\":{\"type\":\"string\",\"description\":\"搜索关键词，必须从用户问题中提取，如用户问'有什么耳机'则keyword='耳机'\"}," +
                "\"categoryId\":{\"type\":\"integer\",\"description\":\"分类ID，可选\"}" +
                "},\"required\":[\"keyword\"]}";
        return new Tool("search_goods", "根据关键词或分类搜索商城商品，返回商品列表",
                parseSchema(schema),
                args -> {
                    String keyword = str(args.get("keyword"));
                    Long categoryId = args.get("categoryId") == null ? null : Long.valueOf(args.get("categoryId").toString());
                    log.info("[Agent工具] search_goods 收到参数: keyword={}, categoryId={}", keyword, categoryId);
                    // 精准搜索：只搜 name+brand，不搜 description
                    if (categoryId != null) {
                        Set<Long> ids = new HashSet<>();
                        ids.add(categoryId);
                        return toJson(spuSearchService.search(ids, keyword, 1, 5));
                    } else {
                        return toJson(spuSearchService.searchByName(keyword, 1, 5));
                    }
                });
    }

    /**
     * 工具二：查询当前登录用户的订单（只查自己的订单，权限隔离）
     */
    @Bean
    public Tool queryOrdersTool() {
        String schema = "{\"type\":\"object\",\"properties\":{" +
                "\"status\":{\"type\":\"string\",\"description\":\"订单状态过滤，可选值：PENDING_PAY/PAID/SHIPPED/RECEIVED/COMPLETED/CANCELLED/REFUNDING/REFUNDED。不填则查全部。\"}," +
                "\"keyword\":{\"type\":\"string\",\"description\":\"商品关键词，只返回订单明细中包含此关键词的订单（如'耳机'，匹配skuName或skuSpec）。不填则返回所有订单。\"}" +
                "},\"required\":[]}";
        return new Tool("query_orders", "查询当前登录用户的订单列表，可按状态/商品关键词过滤",
                parseSchema(schema),
                args -> {
                    Long userId = UserHolder.get();
                    if (userId == null) {
                        return "错误：未登录，无法查询订单";
                    }
                    String status = args.get("status") == null ? null : args.get("status").toString().trim();
                    if (status != null && status.isEmpty()) status = null;
                    String keyword = args.get("keyword") == null ? null : args.get("keyword").toString().trim();
                    if (keyword != null && keyword.isEmpty()) keyword = null;
                    log.info("[Agent工具] query_orders 收到参数: status={}, keyword={}", status, keyword);
                    Result result = orderService.orderQueryListByUser(userId, status, keyword);
                    // 同时把订单摘要写到 AgentContext 给前端卡片用
                    extractOrderCards(result);
                    return formatOrdersForModel(result);
                });
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "序列化失败";
        }
    }

    /**
     * 把任意对象（Map 或 Bean）转成 Map
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> toMap(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Map) return (Map<String, Object>) obj;
        try {
            return objectMapper.convertValue(obj, Map.class);
        } catch (Exception e) {
            log.error("toMap 失败", e);
            return null;
        }
    }

    /**
     * 从订单查询结果中提取订单卡片写入 AgentContext
     */
    private void extractOrderCards(Result result) {
        try {
            Object data = result.getData();
            if (!(data instanceof java.util.List)) return;
            java.util.List<?> list = (java.util.List<?>) data;
            for (Object item : list) {
                Map<String, Object> orderMap = toMap(item);
                if (orderMap == null) continue;
                Map<String, Object> order = toMap(orderMap.get("order"));
                if (order == null) continue;
                AgentOrderCard card = new AgentOrderCard();
                card.setOrderNo(str(order.get("orderNo")));
                card.setStatus(str(order.get("status")));
                Object amount = order.get("totalAmount");
                if (amount != null) {
                    try { card.setTotalAmount(new java.math.BigDecimal(amount.toString())); } catch (Exception ignored) {}
                }
                Object createTime = order.get("createTime");
                if (createTime != null) card.setCreateTime(createTime.toString());

                Object itemsObj = orderMap.get("items");
                if (itemsObj instanceof java.util.List) {
                    java.util.List<?> items = (java.util.List<?>) itemsObj;
                    for (Object o : items) {
                        Map<String, Object> m = toMap(o);
                        if (m == null) continue;
                        AgentOrderCard.OrderItem it = new AgentOrderCard.OrderItem();
                        it.setSkuName(str(m.get("skuName")));
                        it.setMerchantName(str(m.get("merchantName")));
                        Object qty = m.get("quantity");
                        if (qty != null) {
                            try { it.setQuantity(Integer.parseInt(qty.toString())); } catch (Exception ignored) {}
                        }
                        Object price = m.get("skuPrice");
                        if (price != null) {
                            try { it.setSkuPrice(new java.math.BigDecimal(price.toString())); } catch (Exception ignored) {}
                        }
                        card.getItems().add(it);
                    }
                }
                AgentContext.get().addOrder(card);
            }
            log.info("[Agent工具] query_orders 提取订单卡片 {} 张", list.size());
        } catch (Exception e) {
            log.error("extractOrderCards 失败", e);
        }
    }

    /**
     * 把订单查询结果格式化成简洁文本给模型，避免模型直接吐 JSON 字段名
     */
    private String formatOrdersForModel(Result result) {
        try {
            Object data = result.getData();
            if (!(data instanceof java.util.List)) {
                return "暂无订单";
            }
            java.util.List<?> list = (java.util.List<?>) data;
            if (list.isEmpty()) {
                return "暂无订单";
            }
            StringBuilder sb = new StringBuilder();
            sb.append("共").append(list.size()).append("个订单：\n\n");
            int idx = 1;
            for (Object item : list) {
                Map<String, Object> orderMap = toMap(item);
                if (orderMap == null) continue;
                Map<String, Object> order = toMap(orderMap.get("order"));
                String orderNo = order != null && order.get("orderNo") != null ? order.get("orderNo").toString() : "";
                String status = order != null && order.get("status") != null ? order.get("status").toString() : "";
                Object totalAmount = order != null ? order.get("totalAmount") : null;

                sb.append(idx).append(". 订单号：").append(orderNo).append("\n");
                sb.append("   状态：").append(status).append(" | 金额：¥").append(totalAmount == null ? "" : totalAmount).append("\n");
                Object createTime = order != null ? order.get("createTime") : null;
                if (createTime != null) sb.append("   时间：").append(createTime).append("\n");

                Object itemsObj = orderMap.get("items");
                if (itemsObj instanceof java.util.List) {
                    java.util.List<?> items = (java.util.List<?>) itemsObj;
                    for (Object o : items) {
                        Map<String, Object> m = toMap(o);
                        if (m == null) continue;
                        String skuName = m.get("skuName") != null ? m.get("skuName").toString() : "";
                        String merchantName = m.get("merchantName") != null ? m.get("merchantName").toString() : "";
                        Object quantity = m.get("quantity");
                        sb.append("   - ").append(merchantName).append("：").append(skuName);
                        if (quantity != null) sb.append(" ×").append(quantity);
                        sb.append("\n");
                    }
                }
                sb.append("\n");
                idx++;
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("formatOrdersForModel 失败", e);
            return "订单数据格式化失败";
        }
    }

    private String str(Object o) {
        return o == null ? "" : o.toString();
    }

    public Map<String, Object> parseSchema(String json) {
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("schema 解析失败", e);
        }
    }
}