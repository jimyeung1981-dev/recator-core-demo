package com.example.agent.event;

/**
 * 深度思考流式增量事件（对应 DeepSeek 的 reasoning_content 增量输出）
 */
public record ThinkingDeltaEvent(String delta) implements AgentEvent {
    @Override
    public String eventType() {
        return "thinking";
    }
}
