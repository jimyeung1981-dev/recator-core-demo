package com.example.agent.client;

import com.example.agent.model.ChatCompletionChunk;
import com.example.agent.model.ChatMessage;
import com.example.agent.model.ToolDefinition;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 响应式 OpenAI / SiliconFlow 兼容客户端
 * 基于 Spring WebClient + Project Reactor 实现真正的流式 SSE 消费与异步非阻塞调用。
 */
public class ReactiveOpenAiClient {

    private static final Logger log = LoggerFactory.getLogger(ReactiveOpenAiClient.class);

    private final String baseUrl;
    private final String apiKey;
    private final String modelName;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public ReactiveOpenAiClient(String baseUrl, String apiKey, String modelName) {
        this(baseUrl, apiKey, modelName, new ObjectMapper());
    }

    public ReactiveOpenAiClient(String baseUrl, String apiKey, String modelName, ObjectMapper objectMapper) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.modelName = modelName;
        this.objectMapper = objectMapper;

        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * 发起 SSE 响应式流式对话请求
     *
     * @param messages 历史对话消息列表
     * @param tools    可选的工具规范列表
     * @return 实时下发的 ChatCompletionChunk 响应式 Flux 流
     */
    public Flux<ChatCompletionChunk> streamChat(List<ChatMessage> messages, List<ToolDefinition> tools) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", modelName);
        body.put("messages", messages);
        body.put("stream", true);
        if (tools != null && !tools.isEmpty()) {
            body.put("tools", tools);
        }

        return webClient.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(body)
                .retrieve()
                .bodyToFlux(new ParameterizedTypeReference<ServerSentEvent<String>>() {})
                .mapNotNull(ServerSentEvent::data)
                .filter(data -> !data.isBlank())
                .takeWhile(data -> !"[DONE]".equals(data.trim()))
                .handle((data, sink) -> {
                    try {
                        ChatCompletionChunk chunk = objectMapper.readValue(data, ChatCompletionChunk.class);
                        sink.next(chunk);
                    } catch (Exception e) {
                        log.debug("跳过无法解析的 SSE 行: {}", data);
                    }
                });
    }

    /**
     * 发起阻塞式/单次 JSON 对话请求
     *
     * @param messages 历史对话消息列表
     * @param tools    可选的工具规范列表
     * @return 完整的 ChatMessage 单值 Mono 流
     */
    public Mono<ChatMessage> chat(List<ChatMessage> messages, List<ToolDefinition> tools) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", modelName);
        body.put("messages", messages);
        body.put("stream", false);
        if (tools != null && !tools.isEmpty()) {
            body.put("tools", tools);
        }

        return webClient.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(root -> {
                    JsonNode choice = root.path("choices").get(0);
                    JsonNode messageNode = choice.path("message");
                    return objectMapper.convertValue(messageNode, ChatMessage.class);
                });
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getModelName() {
        return modelName;
    }

    public static class Builder {
        private String baseUrl = "https://api.siliconflow.cn/v1";
        private String apiKey;
        private String modelName = "deepseek-ai/DeepSeek-V4.1-Flash";
        private ObjectMapper objectMapper;

        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        public Builder modelName(String modelName) {
            this.modelName = modelName;
            return this;
        }

        public Builder objectMapper(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
            return this;
        }

        public ReactiveOpenAiClient build() {
            if (apiKey == null || apiKey.trim().isEmpty()) {
                throw new IllegalArgumentException("API Key 不能为空！");
            }
            return new ReactiveOpenAiClient(baseUrl, apiKey, modelName,
                    objectMapper != null ? objectMapper : new ObjectMapper());
        }
    }
}
