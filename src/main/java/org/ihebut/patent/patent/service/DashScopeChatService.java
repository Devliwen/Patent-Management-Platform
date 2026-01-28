package org.ihebut.patent.patent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.ihebut.patent.patent.dto.AiChatRequest;
import org.ihebut.patent.patent.dto.AiChatResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

@Service
public class DashScopeChatService {
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String apiKey;
    private final String baseUrl;
    private final String defaultModel;
    private final int timeoutMs;

    public DashScopeChatService(
            ObjectMapper objectMapper,
            @Value("${ai.dashscope.api-key:}") String apiKey,
            @Value("${ai.dashscope.base-url:https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions}") String baseUrl,
            @Value("${ai.dashscope.model:qwen-plus}") String defaultModel,
            @Value("${ai.dashscope.timeout-ms:30000}") int timeoutMs
    ) {
        this.objectMapper = objectMapper;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.baseUrl = baseUrl == null ? "" : baseUrl.trim();
        this.defaultModel = defaultModel == null ? "qwen-plus" : defaultModel.trim();
        this.timeoutMs = timeoutMs;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(timeoutMs))
                .build();
    }

    public AiChatResponse chat(AiChatRequest request) {
        return chatWithMessages(
                request == null ? null : request.getModel(),
                request == null ? null : request.getTemperature(),
                request == null ? null : request.getMaxTokens(),
                request == null ? List.of() : List.of(new Message("user", request.getQuestion()))
        );
    }

    public AiChatResponse chatWithMessages(String modelOverride, Double temperature, Integer maxTokens, List<Message> inputMessages) {
        if (apiKey.isBlank()) {
            throw new IllegalStateException("未配置通义千问API Key（环境变量 DASHSCOPE_API_KEY 或配置 ai.dashscope.api-key）");
        }
        if (baseUrl.isBlank()) {
            throw new IllegalStateException("未配置通义千问请求地址（ai.dashscope.base-url）");
        }

        if (inputMessages == null || inputMessages.isEmpty()) {
            throw new IllegalArgumentException("messages不能为空");
        }

        String model = (modelOverride == null || modelOverride.isBlank()) ? defaultModel : modelOverride.trim();

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("model", model);
        ArrayNode messagesNode = payload.putArray("messages");
        for (Message m : inputMessages) {
            if (m == null) continue;
            if (m.role() == null || m.role().isBlank()) continue;
            if (m.content() == null || m.content().isBlank()) continue;
            ObjectNode msg = objectMapper.createObjectNode();
            msg.put("role", m.role().trim());
            msg.put("content", m.content().trim());
            messagesNode.add(msg);
        }
        if (temperature != null) payload.put("temperature", temperature);
        if (maxTokens != null) payload.put("max_tokens", maxTokens);

        String body;
        try {
            body = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new IllegalStateException("请求序列化失败");
        }

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl))
                .timeout(Duration.ofMillis(timeoutMs))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> resp;
        try {
            resp = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new IllegalStateException("调用通义千问失败：" + e.getMessage());
        }

        if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
            String snippet = resp.body() == null ? "" : resp.body();
            if (snippet.length() > 500) snippet = snippet.substring(0, 500);
            throw new IllegalStateException("通义千问返回异常，HTTP " + resp.statusCode() + "，body=" + snippet);
        }

        try {
            JsonNode root = objectMapper.readTree(resp.body());
            String requestId = root.has("request_id") ? root.path("request_id").asText(null) : root.path("id").asText(null);
            String answer = null;
            JsonNode choices = root.path("choices");
            if (choices.isArray() && !choices.isEmpty()) {
                JsonNode message = choices.get(0).path("message");
                if (message.has("content")) {
                    answer = message.path("content").asText();
                }
            }
            if (answer == null || answer.isBlank()) {
                throw new IllegalStateException("通义千问返回内容为空");
            }
            return new AiChatResponse(answer, model, requestId);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("解析通义千问响应失败");
        }
    }

    public record Message(String role, String content) {
    }
}
