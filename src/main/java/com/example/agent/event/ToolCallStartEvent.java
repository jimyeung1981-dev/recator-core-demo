package com.example.agent.event;

/**
 * 工具调用触发事件（模型决定调用某个本地工具）
 */
public record ToolCallStartEvent(String toolCallId, String toolCallName, String arguments) implements AgentEvent {
    @Override
    public String eventType() {
        return "tool";
    }
}
