package com.example.agent.event;

/**
 * 回答正文流式增量事件（对应 LLM 的 content 增量输出）
 */
public record TextDeltaEvent(String delta) implements AgentEvent {
    @Override
    public String eventType() {
        return "text";
    }
}
