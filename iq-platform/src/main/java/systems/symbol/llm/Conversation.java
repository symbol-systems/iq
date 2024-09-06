package systems.symbol.llm;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import systems.symbol.agent.I_Agentic;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonSerialize
@JsonDeserialize
public class Conversation implements I_Assist<String> {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public List<I_LLMessage<String>> messages = new ArrayList<>();

    public Conversation() {
    }

    public Conversation(I_Assist<String> thread) {
        messages.addAll(thread.messages());
    }

    public Conversation add(I_LLMessage<String> msg) {
//        I_LLMessage<String> last = this.messages.getLast();
//        if (last!=null && last.getRole().equals(msg.getRole())) {
////            this.messages.removeLast();
//            this.messages.add(msg);
//        } else {
//            this.messages.add(msg);
//        }
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

    public String context() {
        return context(2);
    }

    public String context(int window) {
        StringBuilder s$ = new StringBuilder();
        int i = messages.size() - 1;
        int count = 0;

        while (i >= 0 && count < window) {
            I_LLMessage<String> msg = messages.get(i);
            if (msg.getRole() != I_LLMessage.RoleType.system) {
                s$.append(msg.getContent());
                count++;
            }
            i--;
        }

        return s$.toString();
    }

    public I_Assist<String> add(String role, String content) {
        if (content==null||content.isEmpty()) return this;
        this.messages.add(new TextMessage(role, content));
        return this;
    }

    public void system(String content) {
        add("system", content);
    }

    public void user(String content) {
        add("user", content);
    }

    public void assistant(String content) {
        add("assistant", content);
    }

    public String toString() {
        return messages.stream()
                .map(I_LLMessage::toString)
                .collect(Collectors.joining("\n"));
    }

}
