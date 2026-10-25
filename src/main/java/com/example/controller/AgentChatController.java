package com.example.controller;

import com.example.agent.ReactiveReActAgent;
import com.example.agent.event.TextDeltaEvent;
import com.example.agent.event.ThinkingDeltaEvent;
import com.example.agent.event.ToolCallStartEvent;
import com.example.agent.event.ToolResultEvent;
import com.example.tools.AlgorithmTools;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Map;

/**
 * 响应式 Agent 微服务 REST / SSE 接口（基于 Project Reactor Core + WebFlux）
 */
@RestController
@RequestMapping
public class AgentChatController {

    private final ReactiveReActAgent.Builder agentBuilder;
    private final AlgorithmTools algorithmTools;

    public AgentChatController(ReactiveReActAgent.Builder agentBuilder, AlgorithmTools algorithmTools) {
        this.agentBuilder = agentBuilder;
        this.algorithmTools = algorithmTools;
    }

    /**
     * 根路径重定向至 Web 交互界面
     */
    @GetMapping("/")
    public Mono<Void> index(ServerHttpResponse response) {
        response.setStatusCode(HttpStatus.FOUND);
        response.getHeaders().setLocation(URI.create("/index.html"));
        return response.setComplete();
    }

    /**
     * 服务健康检查与元数据信息
     */
    @GetMapping("/api/health")
    public Mono<Map<String, Object>> health() {
        return Mono.just(Map.of(
                "status", "UP",
                "framework", "Project Reactor (reactor-core)",
                "model", "deepseek-ai/DeepSeek-V4.1-Flash",
                "provider", "SiliconFlow",
                "server", "Spring Boot WebFlux"
        ));
    }

    /**
     * SSE 实时流式对话接口
     * 支持实时推送：思考流 (thinking)、工具调用 (tool) 以及文本回复 (text)
     */
    @GetMapping(value = "/api/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chatStream(
            @RequestParam(name = "prompt", defaultValue = "请解释一下 Java 冒泡排序的用途、写法与优劣") String prompt) {

        ReactiveReActAgent agent = agentBuilder.build();

        return agent.streamEvents(prompt)
                .flatMap(event -> {
                    if (event instanceof ThinkingDeltaEvent e) {
                        return Flux.just(ServerSentEvent.<String>builder()
                                .event("thinking")
                                .data(e.delta())
                                .build());
                    } else if (event instanceof ToolCallStartEvent e) {
                        return Flux.just(ServerSentEvent.<String>builder()
                                .event("tool")
                                .data("[调用工具: " + e.toolCallName() + "]")
                                .build());
                    } else if (event instanceof ToolResultEvent e) {
                        return Flux.just(ServerSentEvent.<String>builder()
                                .event("tool_result")
                                .data(e.result())
                                .build());
                    } else if (event instanceof TextDeltaEvent e) {
                        return Flux.just(ServerSentEvent.<String>builder()
                                .event("text")
                                .data(e.delta())
                                .build());
                    }
                    return Flux.empty();
                })
                .concatWith(Mono.just(ServerSentEvent.<String>builder()
                        .event("done")
                        .data("[DONE]")
                        .build()));
    }

    /**
     * 同步/阻塞型 RESTful 对话接口
     */
    @PostMapping(value = "/api/chat", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Map<String, String>> chat(@RequestBody Map<String, String> request) {
        String prompt = request.getOrDefault("prompt", "请解释一下 Java 冒泡排序");
        ReactiveReActAgent agent = agentBuilder.build();

        return agent.call(prompt)
                .map(msg -> Map.of(
                        "role", "assistant",
                        "content", msg.getContent() != null ? msg.getContent() : ""
                ));
    }

    /**
     * 冒泡排序工具直接调用接口
     */
    @GetMapping("/api/tools/bubble-sort")
    public Mono<Map<String, String>> runBubbleSort(
            @RequestParam(name = "numbers", defaultValue = "5, 2, 9, 1, 5, 6") String numbers) {
        String result = algorithmTools.simulateBubbleSort(numbers);
        return Mono.just(Map.of(
                "numbers", numbers,
                "result", result
        ));
    }
}
