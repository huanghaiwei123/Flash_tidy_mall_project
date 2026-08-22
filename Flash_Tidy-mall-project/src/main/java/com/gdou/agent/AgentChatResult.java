package com.gdou.agent;

import com.gdou.pojo.vo.SpuListVo;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Agent 对话返回结果：文本回复 + 结构化商品卡片数据 + 订单卡片数据
 */
@Data
public class AgentChatResult {
    private String content;
    private List<SpuListVo> products;
    private List<AgentOrderCard> orders;

    public AgentChatResult() {
        this.products = new ArrayList<>();
        this.orders = new ArrayList<>();
    }

    public AgentChatResult(String content) {
        this.content = content;
        this.products = new ArrayList<>();
        this.orders = new ArrayList<>();
    }
}
