package systems.symbol.llm.gpt;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import systems.symbol.agent.Facades;
import systems.symbol.llm.Conversation;
import systems.symbol.llm.I_LLMConfig;
import systems.symbol.llm.tools.Tool;
import systems.symbol.tools.APIException;

public class LLMFactoryTestIT {
String authKey = System.getenv("MY_GROQ_API_KEY");
int tokens = 200;

@BeforeEach
public void setUp() {
}

// @Test
public void testGROQ_Llama3_8b() throws IOException, APIException {
I_LLMConfig config = LLMFactory.GROQ_Llama3(tokens);
GPTWrapper ai = new GPTWrapper(authKey, config);
Conversation chat = generateLLMResponse(ai, "hello ...");
assert chat != null;
System.out.printf("llm.llama3.done: %s \n", chat.messages().size());
assert chat.messages.size() > 1;
}

// @Test
public void testGROQ_Llama_DeepSeek() throws IOException, APIException {
I_LLMConfig config = LLMFactory.GROQ_Llama_DeepSeek(tokens);
GPTWrapper ai = new GPTWrapper(authKey, config);
Conversation chat = generateLLMResponse(ai, "what is your nature ...");
assert chat != null;
System.out.printf("llm.llama_deepseek.done: %s \n", chat.messages().size());
assert chat.messages.size() > 1;
}

@Test
public void testGROQ_Llama_Tools() throws IOException, APIException {
I_LLMConfig config = LLMFactory.GROQ_Llama3(tokens);
GPTWrapper ai = new GPTWrapper(authKey, config);
ai.tool(Tool.defineFunction("search_web", "search the web").addStringParam("query", "search query", true)
.build());
ai.tool(Tool.defineFunction("search_memory", "search my memory").addStringParam("query", "SPARQL query", true)
.build());
Conversation chat = generateLLMResponse(ai, "today is " + Facades.time() + ". find todays news");
assert chat != null;
System.out.printf("llm.llama_tools.search: %s \n", chat.messages().size());
assert chat.messages.size() > 0;
Conversation chat2 = generateLLMResponse(ai, "as an RDF agent, self-reflect.");
assert chat2 != null;
System.out.printf("llm.llama_tools.self: %s \n", chat2.messages().size());
assert chat2.messages.size() > 0;
}

Conversation generateLLMResponse(GPTWrapper ai, String say) throws APIException, IOException {
Conversation chat = new Conversation();
chat.user(say);
System.out.printf("llm.gpt.messages: %s ->\n%s\n", say, chat.messages());
ai.complete(chat);
System.out.printf("llm.gpt.complete: %s \n", chat.latest());
assert !chat.messages().isEmpty();
return chat;
}
}