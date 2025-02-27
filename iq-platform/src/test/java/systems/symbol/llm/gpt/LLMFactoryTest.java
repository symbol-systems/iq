package systems.symbol.llm.gpt;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import systems.symbol.llm.I_LLMConfig;

public class LLMFactoryTest {

@BeforeEach
public void setUp() {
}

@Test
public void testGPT3_5_Turbo() {
int tokens = 100;
I_LLMConfig config = LLMFactory.GPT3_5_Turbo(tokens);
assertNotNull(config);
assertEquals(LLMFactory.OPENAI_COMPLETIONS, config.getURL());
assertEquals("gpt-3.5-turbo-0125", config.getName());
assertEquals(tokens, config.getMaxTokens());
}

@Test
public void testGROQ_Llama3_8b() {
int tokens = 200;
I_LLMConfig config = LLMFactory.GROQ_Llama3(tokens);
assertNotNull(config);
assertEquals(LLMFactory.GROQ_COMPLETIONS, config.getURL());
assertEquals("llama-3.1-8b-instant", config.getName());
assertEquals(tokens, config.getMaxTokens());
}

}