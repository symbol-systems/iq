package systems.symbol.llm;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonSerialize
public class ChatThread implements I_Thread<String> {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    List<I_LLMessage<String>> messages = new ArrayList<>();

    public ChatThread() {}

    public ChatThread(I_LLMessage<String> msg) {
        add(msg);
    }

    public ChatThread add(I_LLMessage<String> msg) {
        this.messages.add(msg);
        return this;
    }

    @Override
    public List<I_LLMessage<String>> messages() {
        return messages;
    }

    @Override
    public I_LLMessage<String> latest() {
        return messages.isEmpty()?null:messages.get(messages.size()-1);
    }

    public ChatThread add(String name, String role, String content) {
        this.messages.add(new TextMessage(name, role, content));
        return this;
    }

    public ChatThread system(String content) {
        this.messages.add(new TextMessage("default", "system", content));
        return this;
    }

    public ChatThread user(String content) {
        this.messages.add(new TextMessage("default", "user", content));
        return this;
    }

    public ChatThread ai(String content) {
        this.messages.add(new TextMessage("default", "assistant", content));
        return this;
    }

    public String toString() {
        return messages.stream()
                .map(I_LLMessage::toString)
                .collect(Collectors.joining(", "));
    }
}
