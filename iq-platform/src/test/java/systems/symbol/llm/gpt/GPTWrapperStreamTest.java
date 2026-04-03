package systems.symbol.llm.gpt;

import org.junit.jupiter.api.Test;
import systems.symbol.llm.Conversation;
import systems.symbol.llm.I_LLMConfig;
import systems.symbol.llm.I_LLMessage;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GPTWrapperStreamTest {

@Test
void defaultStreamUsesComplete() throws Exception {
I_LLM<String> local = new I_LLM<>() {
@Override
public java.util.Collection<systems.symbol.llm.tools.Tool> tools() {
return java.util.Collections.emptyList();
}

@Override
public systems.symbol.llm.I_Assist<String> complete(systems.symbol.llm.I_Assist<String> chat) {
Conversation answer = new Conversation();
answer.add(new systems.symbol.llm.TextMessage(I_LLMessage.RoleType.assistant, "ok"));
return answer;
}
};

Conversation chat = new Conversation();
chat.user("Hello");

int count = (int) local.stream(chat).count();
assertEquals(1, count);
}
}
