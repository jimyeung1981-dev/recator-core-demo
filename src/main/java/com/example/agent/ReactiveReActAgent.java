package com.example.agent;

import com.example.agent.client.ReactiveOpenAiClient;
import com.example.agent.event.AgentCompleteEvent;
import com.example.agent.event.AgentEvent;
import com.example.agent.event.TextDeltaEvent;
import com.example.agent.event.ThinkingDeltaEvent;
import com.example.agent.event.ToolCallStartEvent;
import com.example.agent.event.ToolResultEvent;
import com.example.agent.model.ChatCompletionChunk;
import com.example.agent.model.ChatMessage;
import com.example.agent.model.FunctionCall;
import com.example.agent.model.ToolCall;
import com.example.agent.tool.ToolRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * 纯 Project Reactor (reactor-core) 实现的响应式 ReAct Agent
 * 
 * 核心特性：
 * 1. 纯响应式拓扑：完全基于 Flux / Mono、Schedulers 与函数式流转换；
 * 2. 思考流 + 正文流双轨推送：无缝适配 DeepSeek 的 reasoning_content 思考链增量；
 * 3. 动态响应式 ReAct 循环：流式解析 Function Call，调度本地工具执行后，无缝递归下一轮 LLM 推理，直至产出最终答案；
 * 4. 零第三方 Agent 框架依赖：仅需 reactor-core 即可实现同等强大能力。
 */
public class ReactiveReActAgent {

    private static final Logger log = LoggerFactory.getLogger(ReactiveReActAgent.class);

    private final String name;
    private final String sysPrompt;
    private final ReactiveOpenAiClient client;
    private final ToolRegistry toolRegistry;
    private final int maxSteps;

    private ReactiveReActAgent(Builder builder) {
        this.name = builder.name;
        this.sysPrompt = builder.sysPrompt;
        this.client = builder.client;
        this.toolRegistry = builder.toolRegistry != null ? builder.toolRegistry : new ToolRegistry();
        this.maxSteps = builder.maxSteps > 0 ? builder.maxSteps : 5;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * 流式调用 Agent，并下发全流程的 AgentEvent（思考流、工具调用流、工具结果流、文本流及完成事件）
     *
     * @param userPrompt 用户输入提问
     * @return Flux<AgentEvent> 响应式事件流
     */
    public Flux<AgentEvent> streamEvents(String userPrompt) {
        return streamEvents(ChatMessage.user(userPrompt));
    }

    /**
     * 流式调用 Agent，接收结构化用户消息
     */
    public Flux<AgentEvent> streamEvents(ChatMessage userMsg) {
        List<ChatMessage> conversation = new ArrayList<>();
        if (sysPrompt != null && !sysPrompt.isBlank()) {
            conversation.add(ChatMessage.system(sysPrompt));
        }
        conversation.add(userMsg);

        return executeReActLoop(conversation, 0);
    }

    /**
     * 阻塞/单次全量调用 Agent，等待所有思考与工具交互结束后返回最终回答
     */
    public Mono<ChatMessage> call(String userPrompt) {
        return call(ChatMessage.user(userPrompt));
    }

    /**
     * 阻塞/单次全量调用 Agent，接收结构化消息
     */
    public Mono<ChatMessage> call(ChatMessage userMsg) {
        return streamEvents(userMsg)
                .filter(event -> event instanceof AgentCompleteEvent)
                .map(event -> {
                    AgentCompleteEvent completeEvent = (AgentCompleteEvent) event;
                    return ChatMessage.assistant(completeEvent.content());
                })
                .last(ChatMessage.assistant(""));
    }

    /**
     * 核心响应式 ReAct 递归循环
     * 
     * 基于 Reactor 的 Flux.defer + concatWith 实现状态推进与多轮函数调用。
     */
    private Flux<AgentEvent> executeReActLoop(List<ChatMessage> conversation, int step) {
        if (step >= maxSteps) {
            String limitMsg = "\n[提示：已达到最大 ReAct 迭代步数限制 (" + maxSteps + " 步)]";
            return Flux.just(new TextDeltaEvent(limitMsg), new AgentCompleteEvent(limitMsg));
        }

        return Flux.defer(() -> {
            StringBuilder reasoningAcc = new StringBuilder();
            StringBuilder contentAcc = new StringBuilder();
            Map<Integer, ToolCallBuilder> toolCallMap = new LinkedHashMap<>();

            return client.streamChat(conversation, toolRegistry.getToolDefinitions())
                    .flatMap(chunk -> {
                        List<AgentEvent> deltas = new ArrayList<>();

                        // 1. 深度思考增量流
                        String reasoningDelta = chunk.getReasoningDelta();
                        if (reasoningDelta != null && !reasoningDelta.isEmpty()) {
                            reasoningAcc.append(reasoningDelta);
                            deltas.add(new ThinkingDeltaEvent(reasoningDelta));
                        }

                        // 2. 回答文本增量流
                        String textDelta = chunk.getTextDelta();
                        if (textDelta != null && !textDelta.isEmpty()) {
                            contentAcc.append(textDelta);
                            deltas.add(new TextDeltaEvent(textDelta));
                        }

                        // 3. 工具调用增量拼装
                        if (chunk.getToolCalls() != null) {
                            for (ChatCompletionChunk.ToolCallDelta tcDelta : chunk.getToolCalls()) {
                                toolCallMap.computeIfAbsent(tcDelta.getIndex(), k -> new ToolCallBuilder())
                                        .append(tcDelta);
                            }
                        }

                        return Flux.fromIterable(deltas);
                    })
                    .concatWith(Flux.defer(() -> {
                        // 本轮模型流式传输完毕，判断模型是否发起了工具调用
                        List<ToolCall> completedCalls = toolCallMap.values().stream()
                                .map(ToolCallBuilder::build)
                                .filter(Objects::nonNull)
                                .toList();

                        if (completedCalls.isEmpty()) {
                            // 无工具调用，表示当前已是最终回复，ReAct 循环终结
                            String finalContent = contentAcc.toString();
                            conversation.add(ChatMessage.assistant(finalContent));
                            return Flux.just(new AgentCompleteEvent(finalContent));
                        }

                        // 模型发起了工具调用！记录 Assistant 消息
                        conversation.add(ChatMessage.assistantWithToolCalls(contentAcc.toString(), completedCalls));

                        // 依次响应式执行各个工具，下发 ToolCallStartEvent 与 ToolResultEvent
                        return Flux.fromIterable(completedCalls)
                                .concatMap(tc -> {
                                    AgentEvent startEvent = new ToolCallStartEvent(tc.getId(), tc.getName(), tc.getArguments());
                                    return Flux.just(startEvent)
                                            .concatWith(
                                                    toolRegistry.execute(tc.getName(), tc.getArguments())
                                                            .map(result -> {
                                                                // 将工具执行结果记录至上下文
                                                                conversation.add(ChatMessage.tool(tc.getId(), result));
                                                                return new ToolResultEvent(tc.getId(), tc.getName(), result);
                                                            })
                                            );
                                })
                                // 工具全部执行完毕后，携带更新后的上下文进入下一轮 ReAct 推理
                                .concatWith(Flux.defer(() -> executeReActLoop(conversation, step + 1)));
                    }));
        });
    }

    public String getName() {
        return name;
    }

    public String getSysPrompt() {
        return sysPrompt;
    }

    public ReactiveOpenAiClient getClient() {
        return client;
    }

    public ToolRegistry getToolRegistry() {
        return toolRegistry;
    }

    public static class Builder {
        private String name = "ReactorAgent";
        private String sysPrompt;
        private ReactiveOpenAiClient client;
        private ToolRegistry toolRegistry;
        private int maxSteps = 5;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder sysPrompt(String sysPrompt) {
            this.sysPrompt = sysPrompt;
            return this;
        }

        public Builder client(ReactiveOpenAiClient client) {
            this.client = client;
            return this;
        }

        public Builder toolRegistry(ToolRegistry toolRegistry) {
            this.toolRegistry = toolRegistry;
            return this;
        }

        public Builder maxSteps(int maxSteps) {
            this.maxSteps = maxSteps;
            return this;
        }

        public ReactiveReActAgent build() {
            if (client == null) {
                throw new IllegalStateException("ReactiveOpenAiClient 必须配置！");
            }
            return new ReactiveReActAgent(this);
        }
    }

    /**
     * 流式增量 ToolCall 拼装器
     */
    private static class ToolCallBuilder {
        private String id;
        private String type = "function";
        private String name;
        private final StringBuilder arguments = new StringBuilder();

        public void append(ChatCompletionChunk.ToolCallDelta delta) {
            if (delta.getId() != null && !delta.getId().isEmpty()) {
                this.id = delta.getId();
            }
            if (delta.getType() != null && !delta.getType().isEmpty()) {
                this.type = delta.getType();
            }
            if (delta.getFunction() != null) {
                if (delta.getFunction().getName() != null && !delta.getFunction().getName().isEmpty()) {
                    this.name = delta.getFunction().getName();
                }
                if (delta.getFunction().getArguments() != null) {
                    this.arguments.append(delta.getFunction().getArguments());
                }
            }
        }

        public ToolCall build() {
            if (this.name == null || this.name.isEmpty()) {
                return null;
            }
            if (this.id == null || this.id.isEmpty()) {
                this.id = "call_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            }
            return new ToolCall(id, type, new FunctionCall(name, arguments.toString()));
        }
    }
}
