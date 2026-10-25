package com.example.agent.event;

/**
 * Agent 整个 ReAct 推理循环完成事件（包含最终回答全文）
 */
public record AgentCompleteEvent(String content) implements AgentEvent {
    @Override
    public String eventType() {
        return "done";
    }
}
