package com.gdou.controller;

import com.gdou.agent.AgentChatResult;
import com.gdou.agent.AgentService;
import com.gdou.common.Result;
import com.gdou.util.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 智能助手：用户通过对话让 Agent 调用商城接口
 */
@RestController
@RequestMapping("/hhw/assistant")
public class AgentController {

    @Autowired
    private AgentService agentService;

    /**
     * 非流式对话：一次性返回完整回答 + 商品卡片数据
     */
    @PostMapping("/chat")
    public Result chat(@RequestBody Map<String, String> body) {
        String message = body.get("message");
        if (message == null || message.trim().isEmpty()) {
            return Result.Fail("消息内容不能为空");
        }
        AgentChatResult reply = agentService.chat(message.trim());
        return Result.success("回答成功", reply);
    }

    /**
     * 流式对话：SSE 逐字返回，最后附带商品卡片数据
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@RequestBody Map<String, String> body) {
        String message = body.get("message");
        if (message == null || message.trim().isEmpty()) {
            SseEmitter emitter = new SseEmitter();
            try {
                emitter.send(SseEmitter.event().name("error").data("消息内容不能为空"));
            } catch (IOException ignored) {
            }
            emitter.complete();
            return emitter;
        }

        SseEmitter emitter = new SseEmitter(300000L); // 5 分钟超时
        final String finalMessage = message.trim();
        final Long userId = UserHolder.get();

        CompletableFuture.runAsync(() -> {
            try {
                if (userId != null) {
                    UserHolder.set(userId);
                }
                AgentChatResult result = agentService.chatStream(finalMessage, chunk -> {
                    try {
                        emitter.send(SseEmitter.event().name("message").data(chunk));
                    } catch (IOException e) {
                        emitter.completeWithError(e);
                    }
                });

                emitter.send(SseEmitter.event().name("products").data(result.getProducts()));
                emitter.send(SseEmitter.event().name("orders").data(result.getOrders()));
                emitter.send(SseEmitter.event().name("done").data("done"));
                emitter.complete();
            } catch (Exception e) {
                try {
                    emitter.send(SseEmitter.event().name("error").data(e.getMessage()));
                } catch (IOException ignored) {
                }
                emitter.completeWithError(e);
            } finally {
                UserHolder.remove();
            }
        });

        return emitter;
    }
}
