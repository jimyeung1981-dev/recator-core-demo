package com.example.agent.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ToolCall {

    @JsonProperty("id")
    private String id;

    @JsonProperty("type")
    private String type = "function";

    @JsonProperty("function")
    private FunctionCall function;

    public ToolCall() {
    }

    public ToolCall(String id, String type, FunctionCall function) {
        this.id = id;
        this.type = type != null ? type : "function";
        this.function = function;
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

    public FunctionCall getFunction() {
        return function;
    }

    public void setFunction(FunctionCall function) {
        this.function = function;
    }

    public String getName() {
        return function != null ? function.getName() : null;
    }

    public String getArguments() {
        return function != null ? function.getArguments() : null;
    }

    @Override
    public String toString() {
        return "ToolCall{" +
                "id='" + id + '\'' +
                ", type='" + type + '\'' +
                ", function=" + function +
                '}';
    }
}
