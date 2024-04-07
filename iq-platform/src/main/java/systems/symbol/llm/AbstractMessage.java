package systems.symbol.llm;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonSerialize
public class AbstractMessage<T> implements I_LLMessage<T> {
    MessageType type = MessageType.OTHER;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    String role, name;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    T content;

    public AbstractMessage(String name, String role, T content) {
        this.name = name;
        this.role = role;
        this.content = content;
    }

    @Override
    public MessageType getType() {
        return type;
    }

    @Override
    public String getRole() {
        return role;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public T getContent() {
        return content;
    }

    public String toString() {
        return "{user: "+name+", role: "+role+", content: "+content+"}";
    }
}
