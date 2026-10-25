package com.example.agent.tool;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记一个方法为 Agent 可调用的工具
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AgentTool {

    /**
     * 工具全局唯一名称（供大模型识别调用，例如 "simulate_bubble_sort"）
     */
    String name();

    /**
     * 工具功能详细描述，引导 LLM 在合适的上下文触发调用
     */
    String description();
}
