package systems.symbol.llm;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
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

public I_Assist<String> add(String role, String content) {
if (content==null) return this;
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
