package systems.symbol.llm.gpt;

import org.junit.jupiter.api.Test;
import systems.symbol.llm.I_LLM;
import systems.symbol.llm.GPTConfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class LLMFactoryProviderTest {

@Test
void openAiProviderRegisteredAndCreatesWrapper() {
GPTConfig config = new GPTConfig("https://api.openai.com/v1/chat/completions", "gpt-3.5-turbo", 1000);
config.secret = "OPENAI_API_KEY";

I_LLM<String> llm = LLMFactory.provider("openai://api.openai.com", config, "dummy-token");
assertNotNull(llm);
assertEquals(1, llm.tools().size()); // maybe zero but ensure no crash
}

@Test
void groqProviderRegisteredAndCreatesWrapper() {
GPTConfig config = new GPTConfig("https://api.groq.com/v1/llm", "llama-3.1-8b", 1000);
config.secret = "GROQ_API_KEY";

I_LLM<String> llm = LLMFactory.provider("groq://api.groq.com", config, "dummy-token");
assertNotNull(llm);
}
}
