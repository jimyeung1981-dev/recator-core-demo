package com.example.agent.tool;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记 Agent 工具方法的参数元信息
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface ToolParam {

    /**
     * 参数名
     */
    String name();

    /**
     * 参数描述说明
     */
    String description();

    /**
     * 是否为必填项
     */
    boolean required() default true;
}
