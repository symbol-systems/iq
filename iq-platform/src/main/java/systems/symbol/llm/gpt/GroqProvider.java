package systems.symbol.llm.gpt;

import systems.symbol.llm.I_LLM;
import systems.symbol.llm.I_LLMConfig;
import systems.symbol.llm.I_LLMProvider;

public class GroqProvider implements I_LLMProvider {

@Override
public String scheme() {
return "groq";
}

@Override
public I_LLM<String> build(I_LLMConfig config, String token) {
return new GPTWrapper(token, config);
}
}
