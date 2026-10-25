package com.example.agent.event;

/**
 * 工具调用执行完毕事件（包含真实工具执行产出的字符串结果）
 */
public record ToolResultEvent(String toolCallId, String toolCallName, String result) implements AgentEvent {
    @Override
    public String eventType() {
        return "tool_result";
    }
}
