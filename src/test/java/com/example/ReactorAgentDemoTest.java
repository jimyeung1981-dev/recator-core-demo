package com.example;

import com.example.agent.ReactiveReActAgent;
import com.example.agent.client.ReactiveOpenAiClient;
import com.example.agent.event.TextDeltaEvent;
import com.example.agent.tool.ToolRegistry;
import com.example.tools.AlgorithmTools;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.util.concurrent.atomic.AtomicBoolean;

public class ReactorAgentDemoTest {

    @Test
    @DisplayName("测试 AlgorithmTools 冒泡排序执行逻辑")
    public void testAlgorithmToolsBubbleSort() {
        AlgorithmTools tools = new AlgorithmTools();
        String result = tools.simulateBubbleSort("5, 1, 4, 2, 8");

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.contains("[1, 2, 4, 5, 8]"), "排序结果应包含有序数组 [1, 2, 4, 5, 8]");
        Assertions.assertTrue(result.contains("累计比较次数:"), "应包含比较次数日志");
        Assertions.assertTrue(result.contains("累计交换次数:"), "应包含交换次数日志");
    }

    @Test
    @DisplayName("测试 ToolRegistry 响应式工具注册与异步调度执行")
    public void testToolRegistryExecution() {
        ToolRegistry registry = new ToolRegistry();
        registry.registerTool(new AlgorithmTools());

        Assertions.assertEquals(1, registry.getToolDefinitions().size());
        Assertions.assertEquals("simulate_bubble_sort", registry.getToolDefinitions().get(0).getFunction().getName());

        StepVerifier.create(registry.execute("simulate_bubble_sort", "{\"numbers\": \"3, 1, 2\"}"))
                .assertNext(result -> {
                    Assertions.assertNotNull(result);
                    Assertions.assertTrue(result.contains("[1, 2, 3]"));
                    Assertions.assertTrue(result.contains("最终排序结果"));
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("测试纯 Reactor Core ReAct Agent 与 SiliconFlow DeepSeek 模型的响应式流式对话")
    public void testReactorAgentModelCall() {
        ReactiveOpenAiClient client = ReactiveOpenAiClient.builder()
                .baseUrl(ReactorAgentDemo.DEFAULT_BASE_URL)
                .apiKey(ReactorAgentDemo.DEFAULT_API_KEY)
                .modelName(ReactorAgentDemo.DEFAULT_MODEL)
                .build();

        ToolRegistry toolRegistry = new ToolRegistry();
        toolRegistry.registerTool(new AlgorithmTools());

        ReactiveReActAgent agent = ReactiveReActAgent.builder()
                .name("TestReactorAgent")
                .sysPrompt("You are a helpful assistant. Reply concisely.")
                .client(client)
                .toolRegistry(toolRegistry)
                .build();

        StringBuilder responseBuilder = new StringBuilder();
        AtomicBoolean receivedDelta = new AtomicBoolean(false);

        agent.streamEvents("请用一句话简要概括冒泡排序的核心原理。")
                .doOnNext(event -> {
                    if (event instanceof TextDeltaEvent deltaEvent) {
                        receivedDelta.set(true);
                        responseBuilder.append(deltaEvent.delta());
                    }
                })
                .blockLast();

        String response = responseBuilder.toString();
        System.out.println("Test Response:\n" + response);

        Assertions.assertTrue(receivedDelta.get(), "应当成功接收到流式文本输出");
        Assertions.assertFalse(response.trim().isEmpty(), "响应内容不应为空");
    }
}
