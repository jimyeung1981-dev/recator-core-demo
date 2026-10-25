package com.example.agent.tool;

import com.example.agent.model.ToolDefinition;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 响应式工具注册表（基于 Reactor Core）
 * 负责解析带有 @AgentTool 注解的 Java Bean，自动生成符合 OpenAI 规范的 JSON Schema，
 * 并在模型发起工具调用时，通过 Reactor 的 Schedulers.boundedElastic() 异步调度执行。
 */
public class ToolRegistry {

    private static final Logger log = LoggerFactory.getLogger(ToolRegistry.class);

    private final ObjectMapper objectMapper;
    private final Map<String, ToolInvoker> invokers = new ConcurrentHashMap<>();
    private final List<ToolDefinition> definitions = new ArrayList<>();

    public ToolRegistry() {
        this(new ObjectMapper());
    }

    public ToolRegistry(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 注册包含 @AgentTool 注解方法的工具对象
     *
     * @param toolInstance 工具对象实例
     */
    public synchronized void registerTool(Object toolInstance) {
        if (toolInstance == null) {
            return;
        }

        Class<?> clazz = toolInstance.getClass();
        for (Method method : clazz.getDeclaredMethods()) {
            AgentTool toolAnno = method.getAnnotation(AgentTool.class);
            if (toolAnno == null) {
                continue;
            }

            String toolName = toolAnno.name();
            String description = toolAnno.description();

            Map<String, Object> properties = new LinkedHashMap<>();
            List<String> requiredParams = new ArrayList<>();
            List<ParamInfo> paramInfos = new ArrayList<>();

            for (Parameter parameter : method.getParameters()) {
                ToolParam paramAnno = parameter.getAnnotation(ToolParam.class);
                String paramName = paramAnno != null ? paramAnno.name() : parameter.getName();
                String paramDesc = paramAnno != null ? paramAnno.description() : "";
                boolean required = paramAnno == null || paramAnno.required();

                String jsonType = mapJavaTypeToJsonType(parameter.getType());

                Map<String, Object> propDetail = new LinkedHashMap<>();
                propDetail.put("type", jsonType);
                propDetail.put("description", paramDesc);
                properties.put(paramName, propDetail);

                if (required) {
                    requiredParams.add(paramName);
                }

                paramInfos.add(new ParamInfo(paramName, parameter.getType(), required));
            }

            Map<String, Object> schema = new LinkedHashMap<>();
            schema.put("type", "object");
            schema.put("properties", properties);
            if (!requiredParams.isEmpty()) {
                schema.put("required", requiredParams);
            }

            ToolDefinition definition = new ToolDefinition(toolName, description, schema);
            definitions.add(definition);

            method.setAccessible(true);
            invokers.put(toolName, new ToolInvoker(toolInstance, method, paramInfos));

            log.info("已成功注册响应式 Agent 工具: {} -> {}.{}()", toolName, clazz.getSimpleName(), method.getName());
        }
    }

    /**
     * 响应式执行指定工具
     *
     * @param name          工具名称
     * @param argumentsJson 模型输出的参数 JSON 字符串
     * @return Mono<String> 执行结果响应流
     */
    public Mono<String> execute(String name, String argumentsJson) {
        return Mono.fromCallable(() -> {
            ToolInvoker invoker = invokers.get(name);
            if (invoker == null) {
                return "错误：未找到名称为 [" + name + "] 的工具。";
            }
            return invoker.invoke(argumentsJson, objectMapper);
        })
        .subscribeOn(Schedulers.boundedElastic())
        .onErrorResume(e -> {
            log.error("执行工具 [{}] 出现异常: {}", name, e.getMessage(), e);
            return Mono.just("工具执行异常: " + e.getMessage());
        });
    }

    /**
     * 获取所有已注册工具的 OpenAI 规范定义
     */
    public List<ToolDefinition> getToolDefinitions() {
        return definitions;
    }

    private static String mapJavaTypeToJsonType(Class<?> type) {
        if (type == int.class || type == Integer.class || type == long.class || type == Long.class) {
            return "integer";
        } else if (type == double.class || type == Double.class || type == float.class || type == Float.class) {
            return "number";
        } else if (type == boolean.class || type == Boolean.class) {
            return "boolean";
        }
        return "string";
    }

    private record ParamInfo(String name, Class<?> type, boolean required) {}

    private static class ToolInvoker {
        private final Object target;
        private final Method method;
        private final List<ParamInfo> paramInfos;

        public ToolInvoker(Object target, Method method, List<ParamInfo> paramInfos) {
            this.target = target;
            this.method = method;
            this.paramInfos = paramInfos;
        }

        public String invoke(String argumentsJson, ObjectMapper objectMapper) throws Exception {
            JsonNode argsNode = null;
            if (argumentsJson != null && !argumentsJson.trim().isEmpty()) {
                try {
                    argsNode = objectMapper.readTree(argumentsJson);
                } catch (Exception e) {
                    log.warn("解析参数 JSON 失败，将尝试直接作为单参数传递: {}", argumentsJson);
                }
            }

            Object[] args = new Object[paramInfos.size()];
            for (int i = 0; i < paramInfos.size(); i++) {
                ParamInfo pi = paramInfos.get(i);
                if (argsNode != null && argsNode.has(pi.name)) {
                    JsonNode valNode = argsNode.get(pi.name);
                    args[i] = objectMapper.treeToValue(valNode, pi.type);
                } else if (argsNode != null && argsNode.isTextual() && paramInfos.size() == 1) {
                    args[i] = argsNode.asText();
                } else if (paramInfos.size() == 1 && pi.type == String.class) {
                    args[i] = argumentsJson;
                } else {
                    args[i] = null;
                }
            }

            Object result = method.invoke(target, args);
            return result != null ? result.toString() : "执行完成（无返回值）";
        }
    }
}
