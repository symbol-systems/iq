package systems.symbol.llm.tools;

import com.fasterxml.jackson.annotation.JsonProperty;

import systems.symbol.llm.I_ToolSpec;

import java.util.HashMap;
import java.util.Map;

import org.bouncycastle.util.Arrays;

public class FuncTool implements I_ToolSpec {
    @JsonProperty("type")
    private String type = "function";
    // public Bindings function = new SimpleBindings();

    public FuncTool type(String type) {
        this.type = type;
        return this;
    }

    public FuncTool(String name, String description) {
        this.name = name;
        this.description = description;
        this.required = new String[0];
    }

    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    @JsonProperty("parameters")
    private Map<String, FuncToolParam> parameters = new HashMap<>();

    @JsonProperty("returnType")
    private String returnType;

    @JsonProperty("required")
    private String[] required;

    @JsonProperty("errorHandling")
    private String errorHandling;

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getReturnType() {
        return returnType;
    }

    public String getErrorHandling() {
        return errorHandling;
    }

    @Override
    public Map<String, FuncToolParam> getParameters() {
        return parameters;
    }

    public FuncToolParam optional(String type, String name, String description) {
        FuncToolParam param = new FuncToolParam(type, description);
        parameters.put(name, param);
        return param;
    }

    public FuncToolParam requires(String type, String name, String description) {
        FuncToolParam param = new FuncToolParam(type, description);
        parameters.put(name, param);
        required = Arrays.append(required, name);
        return param;
    }

}
