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
GPTWrapper wrapper = new GPTWrapper(token, config);
wrapper.tool(systems.symbol.llm.tools.Tool.defineFunction("groq.chat", "Invoke Groq chat completion").
addStringParam("prompt", "Input text for completion", true).build());
return wrapper;
}
}
