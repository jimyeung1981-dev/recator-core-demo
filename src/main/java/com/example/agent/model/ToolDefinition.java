package com.example.agent.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ToolDefinition {

    @JsonProperty("type")
    private String type = "function";

    @JsonProperty("function")
    private FunctionDefinition function;

    public ToolDefinition() {
    }

    public ToolDefinition(FunctionDefinition function) {
        this.type = "function";
        this.function = function;
    }

    public ToolDefinition(String name, String description, Map<String, Object> parameters) {
        this.type = "function";
        this.function = new FunctionDefinition(name, description, parameters);
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public FunctionDefinition getFunction() {
        return function;
    }

    public void setFunction(FunctionDefinition function) {
        this.function = function;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FunctionDefinition {
        @JsonProperty("name")
        private String name;

        @JsonProperty("description")
        private String description;

        @JsonProperty("parameters")
        private Map<String, Object> parameters;

        public FunctionDefinition() {
        }

        public FunctionDefinition(String name, String description, Map<String, Object> parameters) {
            this.name = name;
            this.description = description;
            this.parameters = parameters;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public Map<String, Object> getParameters() {
            return parameters;
        }

        public void setParameters(Map<String, Object> parameters) {
            this.parameters = parameters;
        }
    }
}
