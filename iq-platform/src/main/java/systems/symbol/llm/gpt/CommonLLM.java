package systems.symbol.llm.gpt;

import systems.symbol.llm.DefaultLLConfig;
import systems.symbol.llm.I_LLMConfig;

public class CommonLLM {

public static final String OPENAI_COMPLETIONS = "https://api.openai.com/v1/chat/completions";
public static final String GROQ_COMPLETIONS = "https://api.groq.com/openai/v1/chat/completions";


public static I_LLMConfig newGPT3_5_Turbo(int tokens) {
return new DefaultLLConfig(OPENAI_COMPLETIONS, "gpt-3.5-turbo-0125", tokens);
}

public static I_LLMConfig newGROQ_Llama3_7b(int tokens) {
return new DefaultLLConfig(GROQ_COMPLETIONS, "llama3-8b-8192", tokens);
}
}
