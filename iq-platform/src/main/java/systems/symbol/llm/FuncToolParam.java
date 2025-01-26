package systems.symbol.llm;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FuncToolParam {

    @JsonProperty("type")
    private String type;

    @JsonProperty("description")
    private String description;

    @JsonProperty("enum")
    private String[] enumValues;

    protected FuncToolParam(String type, String description) {
        this.type = type;
        this.description = description;
    }

    protected FuncToolParam(String type, String description, String[] enums) {
        this.type = type;
        this.description = description;
        this.enumValues = enums;
    }

    public String getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public String[] getEnumValues() {
        return enumValues;
    }

}
