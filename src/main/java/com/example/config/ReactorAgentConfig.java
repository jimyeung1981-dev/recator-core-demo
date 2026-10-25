package com.example.config;

import com.example.agent.ReactiveReActAgent;
import com.example.agent.client.ReactiveOpenAiClient;
import com.example.agent.tool.ToolRegistry;
import com.example.tools.AlgorithmTools;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Reactor Agent Spring Boot 配置类
 */
@Configuration
public class ReactorAgentConfig {

    @Value("${reactor.agent.base-url:https://api.siliconflow.cn/v1}")
    private String baseUrl;

    @Value("${reactor.agent.api-key:sk-fvuvqgswjtkczlhlvxouhhtqxlauqzfspkzrtdgcmbptnakh}")
    private String apiKey;

    @Value("${reactor.agent.model-name:deepseek-ai/DeepSeek-V4.1-Flash}")
    private String modelName;

    @Bean
    public AlgorithmTools algorithmTools() {
        return new AlgorithmTools();
    }

    @Bean
    public ReactiveOpenAiClient reactiveOpenAiClient() {
        return ReactiveOpenAiClient.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(modelName)
                .build();
    }

    @Bean
    public ToolRegistry toolRegistry(AlgorithmTools algorithmTools) {
        ToolRegistry registry = new ToolRegistry();
        registry.registerTool(algorithmTools);
        return registry;
    }

    @Bean
    public ReactiveReActAgent.Builder agentBuilder(ReactiveOpenAiClient reactiveOpenAiClient, ToolRegistry toolRegistry) {
        return ReactiveReActAgent.builder()
                .name("JavaAlgorithmTutor")
                .sysPrompt(
                        "你是一个资深的 Java 架构师与算法讲师。\n"
                                + "在回答 Java 相关技术与算法问题时，请遵循以下要求：\n"
                                + "1. 结构化呈现：分点梳理『核心用途/背景』、『Java 标准实现与优化』、『优劣势剖析（时间/空间复杂度、稳定性）』及『工程实践建议』。\n"
                                + "2. 代码严谨：编写符合生产标准的 Java 代码，关键行加上详细中文注释。\n"
                                + "3. 结合工具：当用户需要具体的数组排序演算或验证时，积极调用 simulate_bubble_sort 工具获取真实执行数据并进行剖析。\n"
                                + "4. 态度严谨客观、生动清晰。"
                )
                .client(reactiveOpenAiClient)
                .toolRegistry(toolRegistry);
    }

    @Bean
    public ReactiveReActAgent defaultAgent(ReactiveReActAgent.Builder agentBuilder) {
        return agentBuilder.build();
    }
}
