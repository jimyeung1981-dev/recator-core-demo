package com.example.agent.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChatCompletionChunk {

    @JsonProperty("id")
    private String id;

    @JsonProperty("model")
    private String model;

    @JsonProperty("choices")
    private List<Choice> choices;

    public ChatCompletionChunk() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public List<Choice> getChoices() {
        return choices;
    }

    public void setChoices(List<Choice> choices) {
        this.choices = choices;
    }

    public String getReasoningDelta() {
        if (choices != null && !choices.isEmpty()) {
            Delta delta = choices.get(0).getDelta();
            if (delta != null) {
                return delta.getReasoningContent();
            }
        }
        return null;
    }

    public String getTextDelta() {
        if (choices != null && !choices.isEmpty()) {
            Delta delta = choices.get(0).getDelta();
            if (delta != null) {
                return delta.getContent();
            }
        }
        return null;
    }

    public List<ToolCallDelta> getToolCalls() {
        if (choices != null && !choices.isEmpty()) {
            Delta delta = choices.get(0).getDelta();
            if (delta != null) {
                return delta.getToolCalls();
            }
        }
        return null;
    }

    public String getFinishReason() {
        if (choices != null && !choices.isEmpty()) {
            return choices.get(0).getFinishReason();
        }
        return null;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Choice {
        @JsonProperty("index")
        private int index;

        @JsonProperty("delta")
        private Delta delta;

        @JsonProperty("finish_reason")
        private String finishReason;

        public int getIndex() {
            return index;
        }

        public void setIndex(int index) {
            this.index = index;
        }

        public Delta getDelta() {
            return delta;
        }

        public void setDelta(Delta delta) {
            this.delta = delta;
        }

        public String getFinishReason() {
            return finishReason;
        }

        public void setFinishReason(String finishReason) {
            this.finishReason = finishReason;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Delta {
        @JsonProperty("role")
        private String role;

        @JsonProperty("content")
        private String content;

        @JsonProperty("reasoning_content")
        private String reasoningContent;

        @JsonProperty("tool_calls")
        private List<ToolCallDelta> toolCalls;

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public String getReasoningContent() {
            return reasoningContent;
        }

        public void setReasoningContent(String reasoningContent) {
            this.reasoningContent = reasoningContent;
        }

        public List<ToolCallDelta> getToolCalls() {
            return toolCalls;
        }

        public void setToolCalls(List<ToolCallDelta> toolCalls) {
            this.toolCalls = toolCalls;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ToolCallDelta {
        @JsonProperty("index")
        private int index;

        @JsonProperty("id")
        private String id;

        @JsonProperty("type")
        private String type;

        @JsonProperty("function")
        private FunctionDelta function;

        public int getIndex() {
            return index;
        }

        public void setIndex(int index) {
            this.index = index;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public FunctionDelta getFunction() {
            return function;
        }

        public void setFunction(FunctionDelta function) {
            this.function = function;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FunctionDelta {
        @JsonProperty("name")
        private String name;

        @JsonProperty("arguments")
        private String arguments;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getArguments() {
            return arguments;
        }

        public void setArguments(String arguments) {
            this.arguments = arguments;
        }
    }
}
