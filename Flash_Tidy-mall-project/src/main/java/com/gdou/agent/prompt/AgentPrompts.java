package com.gdou.agent.prompt;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

/**
 * Agent 提示词管理：从 classpath 资源文件加载系统提示词，动态生成时间提示词
 */
@Component
public class AgentPrompts {

    /** 系统角色提示词文件（resources/prompt/agent-system.txt） */
    @Value("classpath:prompt/agent-system.txt")
    private Resource systemPromptResource;

    private String systemPrompt;

    @PostConstruct
    public void init() {
        try {
            systemPrompt = StreamUtils.copyToString(systemPromptResource.getInputStream(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            systemPrompt = "你是潮汐商城客服。";
        }
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    /** 时间注入模板：让模型能识别"今年"、"上个月"等相对时间词 */
    public static String buildTimePrompt(LocalDateTime now) {
        return "当前时间：" + now + "（今年=" + now.getYear() + "年）";
    }
}