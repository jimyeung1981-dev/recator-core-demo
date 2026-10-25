package com.example.agent.event;

/**
 * 响应式 Agent 事件密封接口（基于 Java 17+ Sealed Interface）
 * 定义了 ReAct Agent 运行过程中通过 Project Reactor Flux 流式下发的所有事件类型。
 */
public sealed interface AgentEvent permits 
        ThinkingDeltaEvent, 
        TextDeltaEvent, 
        ToolCallStartEvent, 
        ToolResultEvent, 
        AgentCompleteEvent {

    /**
     * 获取事件类型标识
     */
    String eventType();
}
