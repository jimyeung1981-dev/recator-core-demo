package com.example;

import com.example.agent.ReactiveReActAgent;
import com.example.agent.client.ReactiveOpenAiClient;
import com.example.agent.event.TextDeltaEvent;
import com.example.agent.event.ThinkingDeltaEvent;
import com.example.agent.event.ToolCallStartEvent;
import com.example.agent.event.ToolResultEvent;
import com.example.agent.tool.ToolRegistry;
import com.example.tools.AlgorithmTools;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Project Reactor Core Agent 独立 CLI 启动演示类
 *
 * 仅使用 reactor-core 与 Spring WebClient，接入 SiliconFlow DeepSeek-V4.1-Flash 模型，
 * 演示具备真实 Tool 调用、Thinking 思考流处理以及流式响应能力的纯响应式 ReAct Agent。
 */
public class ReactorAgentDemo {

    public static final String DEFAULT_BASE_URL = "https://api.siliconflow.cn/v1";
    public static final String DEFAULT_MODEL = "deepseek-ai/DeepSeek-V4.1-Flash";
    public static final String DEFAULT_API_KEY = "sk-fvuvqgswjtkczlhlvxouhhtqxlauqzfspkzrtdgcmbptnakh";

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("           Project Reactor Core (reactor-core) - SiliconFlow Demo               ");
        System.out.println("================================================================================");

        // 1. 获取环境配置
        String apiKey = System.getenv("SILICONFLOW_API_KEY");
        if (apiKey == null || apiKey.trim().isEmpty()) {
            apiKey = System.getenv("OPENAI_API_KEY");
        }
        if (apiKey == null || apiKey.trim().isEmpty()) {
            apiKey = DEFAULT_API_KEY;
        }

        String baseUrl = System.getenv("SILICONFLOW_BASE_URL");
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            baseUrl = DEFAULT_BASE_URL;
        }

        String modelName = System.getenv("SILICONFLOW_MODEL");
        if (modelName == null || modelName.trim().isEmpty()) {
            modelName = DEFAULT_MODEL;
        }

        System.out.printf("[配置] Base URL: %s%n", baseUrl);
        System.out.printf("[配置] Model   : %s%n", modelName);
        System.out.printf("[配置] API Key : %s...%s%n", apiKey.substring(0, 7), apiKey.substring(apiKey.length() - 4));

        // 2. 初始化响应式客户端
        ReactiveOpenAiClient client = ReactiveOpenAiClient.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(modelName)
                .build();

        // 3. 构建响应式工具注册表并挂载冒泡排序工具
        ToolRegistry toolRegistry = new ToolRegistry();
        toolRegistry.registerTool(new AlgorithmTools());

        // 4. 构建基于纯 reactor-core 的 ReAct Agent
        ReactiveReActAgent agent = ReactiveReActAgent.builder()
                .name("JavaAlgorithmTutor")
                .sysPrompt(
                        "你是一个资深的 Java 架构师与算法讲师。\n"
                                + "在回答 Java 相关技术与算法问题时，请遵循以下要求：\n"
                                + "1. 结构化呈现：分点梳理『核心用途/背景』、『Java 标准实现与优化』、『优劣势剖析（时间/空间复杂度、稳定性）』及『工程实践建议』。\n"
                                + "2. 代码严谨：编写符合生产标准的 Java 代码，关键行加上详细中文注释。\n"
                                + "3. 结合工具：当用户需要具体的数组排序演算或验证时，积极调用 simulate_bubble_sort 工具获取真实执行数据并进行剖析。\n"
                                + "4. 态度严谨客观、生动清晰。")
                .client(client)
                .toolRegistry(toolRegistry)
                .build();

        // 5. 第一阶段：自动验证对话
        String targetQuestion = "请详细解释一下 Java 的冒泡排序（Bubble Sort）的用途、写法、优劣。并在合适的地方附上完整的示例代码。";
        System.out.println("\n--------------------------------------------------------------------------------");
        System.out.println("【第一阶段：自动化功能验证】发起问题：");
        System.out.println(" >> " + targetQuestion);
        System.out.println("--------------------------------------------------------------------------------\n");

        askAndStream(agent, targetQuestion);

        // 6. 第二阶段：检查是否开启交互式模式
        boolean interactive = false;
        for (String arg : args) {
            if ("--interactive".equalsIgnoreCase(arg) || "-i".equalsIgnoreCase(arg)) {
                interactive = true;
                break;
            }
        }

        if (interactive) {
            runInteractiveLoop(agent);
        } else {
            System.out.println("\n--------------------------------------------------------------------------------");
            System.out.println("提示：如需进入交互式对话操作，可使用命令行参数: mvn exec:java -Dexec.args=\"--interactive\"");
            System.out.println("--------------------------------------------------------------------------------\n");
            System.exit(0);
        }
    }

    /**
     * 发送提问并通过 Reactor Flux 事件流实时捕获思考过程、工具调用与生成内容
     */
    public static void askAndStream(ReactiveReActAgent agent, String question) {
        AtomicBoolean thinkingHeaderPrinted = new AtomicBoolean(false);
        AtomicBoolean textHeaderPrinted = new AtomicBoolean(false);

        agent.streamEvents(question)
                .doOnNext(event -> {
                    if (event instanceof ThinkingDeltaEvent e) {
                        if (!thinkingHeaderPrinted.getAndSet(true)) {
                            System.out.print("【深度思考 (Reasoning)】:\n");
                        }
                        System.out.print(e.delta());
                    } else if (event instanceof ToolCallStartEvent e) {
                        System.out.printf("\n[调用工具] -> %s (ID: %s, 参数: %s)\n", e.toolCallName(), e.toolCallId(), e.arguments());
                    } else if (event instanceof ToolResultEvent e) {
                        System.out.println("[工具执行结果]:\n" + e.result());
                    } else if (event instanceof TextDeltaEvent e) {
                        if (!textHeaderPrinted.getAndSet(true)) {
                            System.out.print("\n\n【回答内容】:\n");
                        }
                        System.out.print(e.delta());
                    }
                })
                .blockLast();

        System.out.println();
    }

    /**
     * 控制台交互循环
     */
    private static void runInteractiveLoop(ReactiveReActAgent agent) {
        System.out.println("\n================================================================================");
        System.out.println("【第二阶段：交互式操作】已进入终端交互模式，请输入你的问题（输入 'exit' 或 'quit' 退出）：");
        System.out.println("================================================================================");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            while (true) {
                System.out.print("\n你: ");
                String input = reader.readLine();
                if (input == null || input.trim().equalsIgnoreCase("exit") || input.trim().equalsIgnoreCase("quit")) {
                    System.out.println("\n已退出交互模式，再见！");
                    break;
                }
                if (input.trim().isEmpty()) {
                    continue;
                }
                System.out.println();
                askAndStream(agent, input.trim());
            }
        } catch (Exception e) {
            System.err.println("读取控制台输入异常: " + e.getMessage());
        }
    }
}
