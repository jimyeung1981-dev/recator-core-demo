# Project Reactor Core (reactor-core) × Spring Boot Web 微服务 Demo

本项目参考 `/Users/jeff/ws-ref/agentscope-demo` 的功能架构，**完全摒弃了第三方 Agent 框架（如 AgentScope、LangChain4j、Spring AI）**，**只使用 Project Reactor (`reactor-core`) 原生响应式编程模型**，从零手写实现了一套高性能、异步非阻塞的 **ReAct 智能体引擎**，并全面接入 **SiliconFlow** 平台的 `deepseek-ai/DeepSeek-V4.1-Flash` 大模型。

---

## 🌟 核心特性与架构设计

### 1. 纯 `reactor-core` 驱动的 ReAct 智能体引擎
- **纯响应式状态推进**：整个 ReAct 推理过程（用户输入 ➔ 思考链生成 ➔ 函数调用识别 ➔ 本地工具调度 ➔ 结果回填 ➔ 最终内容生成）完全由 `Flux.defer()`、`flatMap()`、`concatWith()` 与响应式递归驱动；
- **异步非阻塞工具调度**：通过 `ToolRegistry` 将 Java 本地方法映射为 OpenAI Function Calling 规范，并通过 `Schedulers.boundedElastic()` 弹性线程池进行非阻塞调度；
- **双轨流式输出**：原生支持 DeepSeek 的 `reasoning_content`（深度思考流）与 `content`（正文流）增量并行推送；
- **零 Agent 框架污染**：代码简洁透明，完全掌握 Agent 的上下文生命周期与事件调度机制。

### 2. 现代响应式微服务与 API
- **SSE 实时流式接口**（`GET /api/chat/stream?prompt=...`）：标准 Server-Sent Events，实时分发 `thinking`、`tool`、`tool_result`、`text` 与 `done` 事件；
- **RESTful 同步对话接口**（`POST /api/chat`）：标准 JSON 格式交互；
- **算法工具直调接口**（`GET /api/tools/bubble-sort?numbers=...`）：直接触发真实 Java 本地排序演练；
- **服务健康探针**（`GET /api/health`）：输出运行时与框架元数据。

### 3. 开箱即用 Web UI 交互界面
- 访问 `http://localhost:8080/` 自动重定向进入现代暗色 Glassmorphism 前端界面；
- 实时呈现思考链（紫光卡片）、工具调用执行追踪（绿光卡片）与逐字打字机排版效果。

---

## 📁 项目目录结构

```
recator-core-demo/
├── pom.xml                                        # Maven 配置（Spring Boot WebFlux + reactor-core）
├── src/
│   ├── main/
│   │   ├── java/com/example/
│   │   │   ├── ReactorAgentApplication.java       # Spring Boot 微服务主启动类
│   │   │   ├── ReactorAgentDemo.java              # 纯 Java / CLI 独立运行模式（支持 -i / --interactive）
│   │   │   ├── agent/
│   │   │   │   ├── ReactiveReActAgent.java        # 核心：纯 reactor-core 实现的 ReAct Agent
│   │   │   │   ├── client/
│   │   │   │   │   └── ReactiveOpenAiClient.java  # 响应式 OpenAI / SiliconFlow SSE 客户端
│   │   │   │   ├── event/
│   │   │   │   │   ├── AgentEvent.java            # 事件密封接口 (Sealed Interface)
│   │   │   │   │   ├── ThinkingDeltaEvent.java    # 深度思考增量事件
│   │   │   │   │   ├── TextDeltaEvent.java        # 回答正文增量事件
│   │   │   │   │   ├── ToolCallStartEvent.java    # 工具调用触发事件
│   │   │   │   │   ├── ToolResultEvent.java       # 工具执行结果事件
│   │   │   │   │   └── AgentCompleteEvent.java    # 对话结束事件
│   │   │   │   ├── model/
│   │   │   │   │   ├── ChatMessage.java           # 统一对话消息模型
│   │   │   │   │   ├── ToolCall.java              # 工具调用模型
│   │   │   │   │   ├── FunctionCall.java          # 函数调用详情
│   │   │   │   │   ├── ToolDefinition.java        # OpenAI Function Calling Schema
│   │   │   │   │   └── ChatCompletionChunk.java   # 流式 Chunk 解析模型
│   │   │   │   └── tool/
│   │   │   │       ├── AgentTool.java             # @AgentTool 工具注解
│   │   │   │       ├── ToolParam.java             # @ToolParam 参数元数据注解
│   │   │   │       └── ToolRegistry.java          # 工具反射注册表与响应式调度器
│   │   │   ├── config/
│   │   │   │   └── ReactorAgentConfig.java        # Spring IOC Bean 装配
│   │   │   ├── controller/
│   │   │   │   └── AgentChatController.java       # REST & SSE 控制器
│   │   │   └── tools/
│   │   │       └── AlgorithmTools.java            # 真实 Java 冒泡排序演算工具
│   │   └── resources/
│   │       ├── application.yml                    # 服务配置参数（SiliconFlow + DeepSeek）
│   │       ├── logback.xml                        # 日志级别配置
│   │       └── static/
│   │           └── index.html                     # 现代 Web 交互前端界面
│   └── test/
│       └── java/com/example/
│           ├── ReactorAgentDemoTest.java          # 核心 Agent 与 StepVerifier 响应式测试
│           └── ReactorAgentApplicationTests.java  # WebTestClient 集成测试
└── README.md
```

---

## 🚀 启动与使用指南

### 1. 启动 Spring Boot 微服务
```bash
mvn spring-boot:run
```
服务将在 `http://localhost:8080` 启动监听。

### 2. 访问 Web 交互界面
在浏览器中直接打开：
👉 **http://localhost:8080/**

可点击预置测试指令或自由输入，实时查看思考链与工具调用。

### 3. 命令行交互模式 (CLI)
通过 Maven 独立运行命令行模式（无需启动 Web 容器）：
```bash
# 执行单次自动化验证
mvn compile exec:java

# 进入终端多轮交互式对话
mvn compile exec:java -Dexec.args="--interactive"
```

### 4. 调用 HTTP API 接口

#### ① SSE 实时流式对话
```bash
curl -N -G "http://localhost:8080/api/chat/stream" \
  --data-urlencode "prompt=请用一句话解释冒泡排序"
```

#### ② JSON 同步调用
```bash
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"prompt": "请用一句话解释冒泡排序"}'
```

#### ③ 本地排序算法工具直调
```bash
curl "http://localhost:8080/api/tools/bubble-sort?numbers=9,3,7,1,5"
```

#### ④ 服务健康检查
```bash
curl http://localhost:8080/api/health
```

### 5. 运行全套自动化测试
```bash
mvn test
```
包含使用 `reactor-test`（`StepVerifier`）对响应式 Agent 事件流的测试，以及基于 `WebTestClient` 的服务端全链路接口测试。
