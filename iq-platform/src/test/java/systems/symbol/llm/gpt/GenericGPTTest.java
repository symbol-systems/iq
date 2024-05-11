package systems.symbol.llm.gpt;

import org.junit.jupiter.api.Test;
import systems.symbol.agent.tools.APIException;
import systems.symbol.llm.Conversation;

import java.io.IOException;

class GenericGPTTest {

@Test
void generateLLMResponse() throws APIException, IOException {
String openaiApiKey = System.getenv("OPENAI_API_KEY");
if (openaiApiKey!=null) {
try {
GenericGPT ai = new GenericGPT(openaiApiKey, 100);
Conversation chat = new Conversation();
chat.user( "hello");
System.out.println("agent.llm.openai.messages: "+chat.messages());
ai.complete(chat);
System.out.println("agent.llm.openai: "+chat);
assert !chat.messages().isEmpty();
} catch (java.net.UnknownHostException e) {
System.out.println("agent.llm.openai.offline");
}
}
}
}