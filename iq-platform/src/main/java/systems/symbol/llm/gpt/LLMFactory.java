package systems.symbol.llm.gpt;

import java.io.FileOutputStream;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import systems.symbol.llm.I_LLM;
import systems.symbol.llm.I_LLMConfig;
import systems.symbol.platform.Poke;
import systems.symbol.secrets.I_Secrets;
import systems.symbol.secrets.SecretsException;
import systems.symbol.string.PrettyString;

public class LLMFactory {
static protected final Logger log = LoggerFactory.getLogger(LLMFactory.class);

public static final String OPENAI_COMPLETIONS = "https://api.openai.com/v1/chat/completions";
public static final String GROQ_COMPLETIONS = "https://api.groq.com/openai/v1/chat/completions";

public static I_LLMConfig GPT3_5_Turbo(int tokens) {
return new GPTConfig(OPENAI_COMPLETIONS, "gpt-3.5-turbo-0125", tokens);
}

public static I_LLMConfig GROQ_Llama3(int tokens) {
return new GPTConfig(GROQ_COMPLETIONS, "llama-3.1-8b-instant", tokens);
}

public static I_LLMConfig GROQ_Llama_DeepSeek(int tokens) {
return new GPTConfig(GROQ_COMPLETIONS, "deepseek-r1-distill-llama-70b", tokens);
}

public static I_LLMConfig configure(Resource self, Model model, int contextLength) {
GPTConfig config = new GPTConfig(contextLength);
try {
Poke.poke(self, model, config);
} catch (IllegalAccessException e) {
throw new RuntimeException(e);
}
return config;
}

public static GPTWrapper llm(Resource self, Model model, int contextLength, I_Secrets secrets)
throws SecretsException {
I_LLMConfig config = configure(self, model, contextLength);
if (config.getName() == null || config.getURL() == null) {
log.error("llm.config.missing: {} -> {} -> {}", self, config.getName(), config.getURL());
return null;
}
String secretName = PrettyString.localName(config.getSecretName());
if (secretName == null || secretName.isEmpty())
throw new SecretsException("secret.config.missing: " + config.getName());
String token = secrets.getSecret(secretName);
if (token == null)
throw new SecretsException("secret.missing: " + secretName);
log.info("llm.gpt: {} -> {} @ {} as {}", self, secretName, config.getURL(), config.getResponseFormat());
return new GPTWrapper(token, config);
}

}
