package com.gdou.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Component
public class OllamaClient {
    private final String baseUrl;
    private final String model;
    private final Double temperature;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public OllamaClient(@Value("${ollama.base-url}")String baseUrl,
                        @Value("${ollama.model}")String model,
                        @Value("${ollama.temperature}")Double temperature) {
        this.baseUrl = baseUrl;
        this.model = model;
        this.temperature = temperature;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 调用模型，返回完整响应（用于工具调用阶段）
     */
    public Map<String,Object> chat(List<Map<String, Object>> messages, List<Map<String, Object>> tools){
        Map<String,Object> body = buildBody(messages, tools, false);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(toJson(body), headers);

        ResponseEntity<String> resp = restTemplate.postForEntity(
                baseUrl + "/api/chat", entity, String.class);
        return parseJson(resp.getBody());
    }

    /**
     * 流式调用模型，逐段回调文本内容
     */
    public void chatStream(List<Map<String, Object>> messages,
                           List<Map<String, Object>> tools,
                           Consumer<String> onChunk) {
        Map<String, Object> body = buildBody(messages, tools, true);
        String jsonBody = toJson(body);

        HttpURLConnection conn = null;
        try {
            URL url = new URL(baseUrl + "/api/chat");
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(120000);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.trim().isEmpty()) continue;
                    try {
                        Map<String, Object> chunk = parseJson(line);
                        Map<String, Object> message = (Map<String, Object>) chunk.get("message");
                        if (message != null) {
                            Object content = message.get("content");
                            if (content != null && !content.toString().isEmpty()) {
                                onChunk.accept(content.toString());
                            }
                        }
                    } catch (Exception e) {
                        // 忽略无法解析的行
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Ollama 流式调用失败", e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private Map<String, Object> buildBody(List<Map<String, Object>> messages,
                                          List<Map<String, Object>> tools,
                                          boolean stream) {
        Map<String,Object> body = new HashMap<>();
        body.put("messages", messages);
        body.put("model", model);
        body.put("temperature", temperature);
        body.put("stream", stream);
        if(tools != null && !tools.isEmpty()){
            body.put("tools", tools);
        }
        return body;
    }

    private String toJson(Object obj) {
        try { return objectMapper.writeValueAsString(obj); }
        catch (Exception e) { throw new RuntimeException("JSON 序列化失败", e); }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJson(String json) {
        try { return objectMapper.readValue(json, Map.class); }
        catch (Exception e) { throw new RuntimeException("JSON 解析失败: " + json, e); }
    }
}
