package com.gdou.agent;

import com.gdou.pojo.vo.SpuListVo;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Agent 单次对话的上下文：收集工具产生的商品/订单卡片数据
 * 用 ThreadLocal 隔离并发请求
 */
@Component
public class AgentContext {
    private static final ThreadLocal<AgentContext> HOLDER = new ThreadLocal<>();

    private final List<SpuListVo> products = new ArrayList<>();
    private final List<AgentOrderCard> orders = new ArrayList<>();

    public static AgentContext get() {
        AgentContext ctx = HOLDER.get();
        if (ctx == null) {
            ctx = new AgentContext();
            HOLDER.set(ctx);
        }
        return ctx;
    }

    public static void clear() {
        HOLDER.remove();
    }

    public void addProduct(SpuListVo v) { products.add(v); }
    public void addOrder(AgentOrderCard o) { orders.add(o); }

    public List<SpuListVo> getProducts() { return products; }
    public List<AgentOrderCard> getOrders() { return orders; }
}
