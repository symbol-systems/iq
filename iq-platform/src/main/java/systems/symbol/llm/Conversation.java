package systems.symbol.llm;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonSerialize
@JsonDeserialize
public class Conversation implements I_Assist<String> {

@JsonInclude(JsonInclude.Include.NON_NULL)
public List<I_LLMessage<String>> messages = new ArrayList<>();

@JsonInclude(JsonInclude.Include.NON_NULL)
public Map<String, Object> meta = new HashMap<>();

public Conversation() {
}

public Conversation(I_Assist<String> chat) {
messages.addAll(chat.messages());
}

public Conversation(I_Assist<String> chat, int window) {
add(chat, window);
}

public Conversation add(I_LLMessage<String> msg) {
// I_LLMessage<String> last = this.messages.getLast();
// if (last!=null && last.getRole().equals(msg.getRole())) {
//// this.messages.removeLast();
// this.messages.add(msg);
// } else {
// this.messages.add(msg);
// }
this.messages.add(msg);
return this;
}

@Override
public List<I_LLMessage<String>> messages() {
return messages;
}

@Override
public I_LLMessage<String> latest() {
return messages.isEmpty() ? null : messages.get(messages.size() - 1);
}

// public String context() {
// return context(2);
// }

// public String context(int window) {
// StringBuilder s$ = new StringBuilder();
// int i = messages.size() - 1;
// int count = 0;

// while (i >= 0 && count < window) {
// I_LLMessage<String> msg = messages.get(i);
// if (msg.getRole() != I_LLMessage.RoleType.system) {
// String content = msg.getContent();
// if (content != null)
// s$.append(content);
// count++;
// }
// i--;
// }

// return s$.toString();
// }

public void add(I_Assist<String> chat, int window) {
if (chat == null || chat.messages() == null || window <= 0) {
return;
}

List<I_LLMessage<String>> messages = chat.messages();
int size = messages.size();

if (size == 0) {
return;
}

int startIdx = Math.max(0, size - window);

for (int i = startIdx; i < size; i++) {
I_LLMessage<String> msg = messages.get(i);
if (msg != null && msg.getRole() != I_LLMessage.RoleType.system) {
String content = msg.getContent();
if (content != null) {
add(msg);
}
}
}
}

public I_Assist<String> add(String role, String content) {
if (content == null || content.isEmpty())
return this;
this.messages.add(new TextMessage(role, content));
return this;
}

public void system(String content) {
if (content == null)
return;
add("system", content);
}

public void user(String content) {
if (content == null)
return;
add("user", content);
}

public void assistant(String content) {
if (content == null)
return;
add("assistant", content);
}

public Map<String, Object> getBindings() {
return meta;
}

public String toString() {
return messages.stream()
.map(I_LLMessage::toString)
.collect(Collectors.joining("\n"));
}

}
