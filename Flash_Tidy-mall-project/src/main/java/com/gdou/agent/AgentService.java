package com.gdou.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gdou.agent.prompt.AgentPrompts;
import com.gdou.common.Result;
import com.gdou.pojo.vo.SpuListVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Service
public class AgentService {
    @Autowired
    private OllamaClient ollamaClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private List<Tool> tools;  //Spring会自动注入所有Tool类型的bean

    @Autowired
    private AgentPrompts agentPrompts;

    /** 最多工具调用轮数，防止死循环 */
    private static final int MAX_TURNS = 5;

    /**
     * 非流式对话：返回完整文本 + 商品卡片数据
     */
    public AgentChatResult chat(String userMessage) {
        AgentContext.clear();
        ChatContext ctx = buildContext(userMessage);

        for (int i = 0; i < MAX_TURNS; i++) {
            Map<String, Object> response = ollamaClient.chat(ctx.messages, ctx.toolSchema);
            Map<String, Object> message = (Map<String, Object>) response.get("message");

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> toolCalls = (List<Map<String, Object>>) message.get("tool_calls");
            if (toolCalls == null || toolCalls.isEmpty()) {
                Object content = message.get("content");
                String text = content == null ? "（模型没有返回内容）" : content.toString();
                return buildResult(text);
            }

            ctx.messages.add(message);
            executeToolCalls(ctx, toolCalls);
        }
        return buildResult("抱歉，我处理这个问题太久了，请换个问法试试。");
    }

    /**
     * 流式对话：工具调用走非流式循环，最终回答以 SSE 流式返回
     * @param onContent 每个文本片段的回调
     */
    public AgentChatResult chatStream(String userMessage, Consumer<String> onContent) {
        AgentContext.clear();
        ChatContext ctx = buildContext(userMessage);

        // 先完成所有工具调用（工具调用不适合流式）
        for (int i = 0; i < MAX_TURNS; i++) {
            Map<String, Object> response = ollamaClient.chat(ctx.messages, ctx.toolSchema);
            Map<String, Object> message = (Map<String, Object>) response.get("message");

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> toolCalls = (List<Map<String, Object>>) message.get("tool_calls");
            if (toolCalls == null || toolCalls.isEmpty()) {
                // 没有工具调用了，流式输出最终回答
                StringBuilder sb = new StringBuilder();
                ollamaClient.chatStream(ctx.messages, ctx.toolSchema, chunk -> {
                    sb.append(chunk);
                    onContent.accept(chunk);
                });
                return buildResult(sb.toString());
            }

            ctx.messages.add(message);
            executeToolCalls(ctx, toolCalls);
        }

        onContent.accept("抱歉，我处理这个问题太久了，请换个问法试试。");
        return buildResult("抱歉，我处理这个问题太久了，请换个问法试试。");
    }

    private AgentChatResult buildResult(String text) {
        AgentChatResult result = new AgentChatResult(text);
        AgentContext ctx = AgentContext.get();
        result.setProducts(ctx.getProducts());
        result.setOrders(ctx.getOrders());
        return result;
    }

    private ChatContext buildContext(String userMessage) {
        ChatContext ctx = new ChatContext();
        ctx.userMessage = userMessage;

        Map<String, Object> systemMap = new HashMap<>();
        systemMap.put("role", "system");
        systemMap.put("content", agentPrompts.getSystemPrompt());
        ctx.messages.add(systemMap);

        // 注入当前时间，让模型能识别"今年"、"上个月"等时间词
        Map<String, Object> timeMap = new HashMap<>();
        timeMap.put("role", "system");
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        timeMap.put("content", AgentPrompts.buildTimePrompt(now));
        ctx.messages.add(timeMap);

        Map<String, Object> userMap = new HashMap<>();
        userMap.put("role", "user");
        userMap.put("content", userMessage);
        ctx.messages.add(userMap);

        for (Tool tool : tools) {
            ctx.toolSchema.add(tool.toSchema());
        }
        return ctx;
    }

    private void executeToolCalls(ChatContext ctx, List<Map<String, Object>> toolCalls) {
        for (Map<String, Object> toolCall : toolCalls) {
            Map<String, Object> function = (Map<String, Object>) toolCall.get("function");
            String name = function.get("name").toString();
            Map<String, Object> args = parseArgs(function.get("arguments").toString());

            // 模型经常不填 keyword，兜底：用用户原始问题提取搜索词
            if ("search_goods".equals(name) || "query_orders".equals(name)) {
                String kw = args.get("keyword") == null ? "" : args.get("keyword").toString().trim();
                if (kw.isEmpty()) {
                    kw = extractKeyword(ctx.userMessage);
                    // 提取出时间词（今年/去年/上个月）就不要兜底，避免给订单工具传入非商品名
                    if (!kw.isEmpty() && !isTimeWord(kw)) {
                        args.put("keyword", kw);
                    }
                }
            }

            Tool tool = findTool(name);
            String result;
            if (tool == null) {
                result = "错误，找不到对应的工具";
            } else {
                result = tool.execute(args);
            }

            // 如果是搜索商品，解析出商品列表，并把更友好的 markdown 表格喂给模型
            if ("search_goods".equals(name)) {
                try {
                    Result<?> rawResult = objectMapper.readValue(result, Result.class);
                    Object dataObj = rawResult.getData();
                    if (dataObj instanceof Map) {
                        Object listObj = ((Map<?, ?>) dataObj).get("records");
                        if (listObj instanceof List) {
                            List<?> rawList = (List<?>) listObj;
                            String kw = args.get("keyword") == null ? "" : args.get("keyword").toString().trim();
                            List<SpuListVo> products = new ArrayList<>();
                            for (Object item : rawList) {
                                SpuListVo vo = objectMapper.convertValue(item, SpuListVo.class);
                                // 后过滤：name 或 brand 必须包含关键词，否则排除（防手机/唇膏混入）
                                if (!kw.isEmpty() && !containsKeyword(vo, kw)) {
                                    continue;
                                }
                                products.add(vo);
                            }
                            for (SpuListVo p : products) AgentContext.get().addProduct(p);
                            result = formatProductsForModel(products);
                        }
                    }
                } catch (Exception e) {
                    // 解析失败时仍用原始 JSON 结果
                }
            }

            Map<String, Object> toolMap = new HashMap<>();
            toolMap.put("role", "tool");
            toolMap.put("name", name);
            toolMap.put("content", result);
            ctx.messages.add(toolMap);
        }
    }

    private String formatProductsForModel(List<SpuListVo> products) {
        if (products == null || products.isEmpty()) {
            return "未搜索到相关商品。";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("共找到").append(products.size()).append("件商品：\n\n");
        int idx = 1;
        for (SpuListVo p : products) {
            sb.append(idx).append(". **").append(nullSafe(p.getName())).append("**\n");
            sb.append("品牌：").append(nullSafe(p.getBrand()));
            sb.append(" | 价格：¥").append(formatPrice(p.getMinPrice()));
            sb.append(" | 销量：").append(p.getSales() == null ? 0 : p.getSales());
            sb.append("\n\n");
            idx++;
        }
        return sb.toString();
    }

    private String nullSafe(String s) {
        return s == null ? "" : s;
    }

    private String formatPrice(BigDecimal price) {
        if (price == null) return "-";
        return price.stripTrailingZeros().toPlainString();
    }

    private Tool findTool(String name) {
        for (Tool tool : tools) {
            if (tool.getName().equals(name)) {
                return tool;
            }
        }
        return null;
    }

    private Map<String, Object> parseArgs(String json) {
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    /**
     * 检查商品 name/brand 是否包含关键词（或关键词的任意2字连续片段）
     */
    private boolean containsKeyword(SpuListVo vo, String keyword) {
        if (vo == null || keyword == null || keyword.isEmpty()) return true;
        String name = vo.getName() == null ? "" : vo.getName();
        String brand = vo.getBrand() == null ? "" : vo.getBrand();
        // 完全包含
        if (name.contains(keyword) || brand.contains(keyword)) return true;
        // 关键词 >=2 字时，检查连续 2 字片段（防"蓝牙耳机"匹配不到"耳机"）
        if (keyword.length() >= 2) {
            for (int i = 0; i < keyword.length() - 1; i++) {
                String sub = keyword.substring(i, i + 2);
                if (name.contains(sub) || brand.contains(sub)) return true;
            }
        }
        return false;
    }

    /**
     * 从用户问题里提取搜索关键词，去掉问句和意图词
     */
    private boolean isTimeWord(String word) {
        if (word == null || word.isEmpty()) return false;
        String[] times = {"今年", "去年", "前年", "明年", "后年", "上个月", "这个月", "上一年",
                "今天", "昨天", "明天", "上周", "本周", "这个星期", "上星期", "本月", "上月",
                "最近", "之前", "以前", "当前", "现在"};
        for (String t : times) {
            if (word.equals(t) || word.contains(t)) return true;
        }
        return false;
    }

    private String extractKeyword(String userMessage) {
        if (userMessage == null) return "";
        String msg = userMessage.trim();
        String[] stopWords = {
                "请", "帮我", "给我", "推荐", "介绍", "看看", "找一下", "搜索", "搜一下",
                "有没有", "有哪些", "哪些", "哪款", "什么牌子", "什么的", "什么",
                "我的", "你们的", "我们的", "这里", "这里有", "看看",
                "订单", "里面", "里面的", "里", "下", "下单",
                "的", "吗", "呢", "啊", "吧", "哟", "哇", "？", "?", "，", ",", "。", "！", "!", "、",
                "有", "我想买", "我想", "可以", "能", "是在", "在"
        };
        for (String w : stopWords) {
            msg = msg.replace(w, " ");
        }
        msg = msg.replaceAll("\\s+", " ").trim();
        return msg;
    }

    private static class ChatContext {
        String userMessage = "";
        List<Map<String, Object>> messages = new ArrayList<>();
        List<Map<String, Object>> toolSchema = new ArrayList<>();
    }
}
