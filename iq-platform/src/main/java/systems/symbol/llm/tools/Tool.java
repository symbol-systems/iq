package systems.symbol.llm.tools;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Tool {
private ToolType type;
private FunctionDef function;

// Getters and Setters
public ToolType getType() {
return type;
}

public void setType(ToolType type) {
this.type = type;
}

public FunctionDef getFunction() {
return function;
}

public void setFunction(FunctionDef function) {
this.function = function;
}

public enum ToolType {
@JsonProperty("function")
FUNCTION
}

public String toString() {
return function.getName();
}

public static FunctionBuilder defineFunction(String name, String description) {
return new FunctionBuilder(name, description);
}

public static class FunctionBuilder {
private final String functionName;
private final String functionDescription;
private final Map<String, ParameterSchema> parameters = new LinkedHashMap<>();
private final List<String> requiredParams = new ArrayList<>();

public FunctionBuilder(String name, String description) {
this.functionName = name;
this.functionDescription = description;
}

public FunctionBuilder addStringParam(String name, String paramDescription, boolean required) {
ParameterSchema schema = new ParameterSchema();
schema.setType("string");
schema.setDescription(paramDescription);
parameters.put(name, schema);
if (required)
requiredParams.add(name);
return this;
}

public FunctionBuilder addEnumParam(String name, List<String> enumValues, String paramDescription,
boolean required) {
ParameterSchema schema = new ParameterSchema();
schema.setType("string");
schema.setEnumValues(enumValues);
schema.setDescription(paramDescription);
parameters.put(name, schema);
if (required)
requiredParams.add(name);
return this;
}

public FunctionBuilder addNumberParam(String name, String paramDescription, boolean required) {
ParameterSchema schema = new ParameterSchema();
schema.setType("number");
schema.setDescription(paramDescription);
parameters.put(name, schema);
if (required)
requiredParams.add(name);
return this;
}

public Tool build() {
// Create the root parameter schema
ParameterSchema rootSchema = new ParameterSchema();
rootSchema.setType("object");
rootSchema.setProperties(parameters);
rootSchema.setRequired(requiredParams);

// Create the function definition
FunctionDef functionDef = new FunctionDef();
functionDef.setName(functionName);
functionDef.setDescription(functionDescription);
functionDef.setParameters(rootSchema);

// Create the tool
Tool tool = new Tool();
tool.setType(ToolType.FUNCTION);
tool.setFunction(functionDef);
return tool;
}
}
}

@JsonInclude(JsonInclude.Include.NON_NULL)
class FunctionDef {
private String name;
private String description;
private ParameterSchema parameters;

// Getters and Setters
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

public ParameterSchema getParameters() {
return parameters;
}

public void setParameters(ParameterSchema parameters) {
this.parameters = parameters;
}
}

@JsonInclude(JsonInclude.Include.NON_NULL)
class ParameterSchema {
private String type;
private String description;

@JsonProperty("enum")
private List<String> enumValues;

private Map<String, ParameterSchema> properties;
private List<String> required;

public String getType() {
return type;
}

public void setType(String type) {
this.type = type;
}

public String getDescription() {
return description;
}

public void setDescription(String description) {
this.description = description;
}

public List<String> getEnumValues() {
return enumValues;
}

public void setEnumValues(List<String> enumValues) {
this.enumValues = enumValues;
}

public Map<String, ParameterSchema> getProperties() {
return properties;
}

public void setProperties(Map<String, ParameterSchema> properties) {
this.properties = properties;
}

public List<String> getRequired() {
return required;
}

public void setRequired(List<String> required) {
this.required = required;
}
}