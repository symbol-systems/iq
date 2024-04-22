package systems.symbol.aspects;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import systems.symbol.agent.LLMAgent;
import systems.symbol.agent.ScriptAgent;
import systems.symbol.fsm.StateException;
import systems.symbol.intent.Executive;
import systems.symbol.intent.I_Intent;
import systems.symbol.llm.openai.ChatGPT;
import systems.symbol.secrets.I_Secrets;
import systems.symbol.secrets.SecretsException;

public class LLMAgents {
static String ENV_OPENAI_API_KEY = "OPENAI_API_KEY";
static String ENV_GROQ_API_KEY = "GROQ_API_KEY";

public static LLMAgent openai(IRI self, Model model, I_Secrets secrets, int tokens) throws SecretsException, StateException {
return openai(self, "default", model, secrets, tokens, new Executive(self, model));
}

public static LLMAgent openai(IRI self, String agent, Model model, I_Secrets secrets, int tokens, I_Intent executive) throws SecretsException, StateException {
String token = secrets.getSecret(ENV_OPENAI_API_KEY);
ChatGPT chatGPT = new ChatGPT(token, tokens);
ScriptAgent scriptAgent = new ScriptAgent(model, self);
return new LLMAgent(chatGPT, scriptAgent);
}

public static LLMAgent groq(IRI self, Model model, I_Secrets secrets, int tokens) throws SecretsException, StateException {
return groq(self, "default", model, secrets, tokens, new Executive(self, model));
}
public static LLMAgent groq(IRI self, String agent, Model model, I_Secrets secrets, int tokens, I_Intent executive) throws SecretsException, StateException {
String token = secrets.getSecret(ENV_GROQ_API_KEY);
ChatGPT chatGPT = new ChatGPT(token, tokens);
ScriptAgent scriptAgent = new ScriptAgent(model, self);
return new LLMAgent(chatGPT, scriptAgent);
}
}
