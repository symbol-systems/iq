package systems.symbol.llm;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonSerialize
public class AbstractMessage<T> implements I_LLMessage<T> {

    public AbstractMessage() {
    }
    MessageType type = MessageType.error;
//    @JsonInclude(JsonInclude.Include.NON_NULL)
//    String name;
    RoleType role;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    T content;

    public AbstractMessage(String role, T content) {
        this.role = RoleType.valueOf(role.toLowerCase());
        this.content = content;
    }

    public AbstractMessage(RoleType r, T s) {
        this.role = r;
        this.content = s;
    }

    @Override
    public MessageType getType() {
        return type;
    }

    @Override
    public RoleType getRole() {
        return role;
    }


    @Override
    public T getContent() {
        return content;
    }

    public String toString() {
        return "{role: "+role+", content: "+content+"}";
    }
}
