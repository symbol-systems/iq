package systems.symbol.llm;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonSerialize
public class TextMessage extends AbstractMessage<String> {

    public TextMessage() {
    }

    @JsonCreator
    public TextMessage(
                       @JsonProperty("role") String role,
                       @JsonProperty("content") String content) {
        this.type = MessageType.text;
        this.role = RoleType.valueOf(role);
        this.content = content;
    }

    public TextMessage(RoleType roleType, String prompt) {
        this.type = MessageType.text;
        this.role = roleType;
        this.content = prompt;
    }
}
