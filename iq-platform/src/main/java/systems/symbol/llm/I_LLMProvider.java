package systems.symbol.llm;

public interface I_LLMProvider {

/**
 * PEM scheme string identifier, e.g. "openai", "groq", "ollama", "anthropic".
 */
String scheme();

/**
 * Build a provider implementation given config.
 */
I_LLM<String> build(I_LLMConfig config, String token);

}
