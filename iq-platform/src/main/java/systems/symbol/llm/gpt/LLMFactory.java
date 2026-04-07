package systems.symbol.llm.gpt;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import systems.symbol.IQConstants;
import systems.symbol.llm.I_LLM;
import systems.symbol.llm.I_LLMConfig;
import systems.symbol.llm.I_LLMProvider;
import systems.symbol.platform.Poke;
import systems.symbol.secrets.I_Secrets;
import systems.symbol.secrets.SecretsException;
import systems.symbol.string.PrettyString;

/**
 * Factory for creating and configuring LLM (Large Language Model) providers.
 * 
 * Provides factory methods for popular LLM models (GPT-3.5, Llama, etc.) and
 * handles RDF-based configuration loading with secure secret management.
 * 
 * @author Symbol Systems
 */
public class LLMFactory {
static protected final Logger log = LoggerFactory.getLogger(LLMFactory.class);

public static final String OPENAI_COMPLETIONS = IQConstants.OPENAI_COMPLETIONS;
public static final String GROQ_COMPLETIONS = IQConstants.GROQ_COMPLETIONS;

public static I_LLMConfig GPT3_5_Turbo(int tokens) {
return new GPTConfig(OPENAI_COMPLETIONS, "gpt-3.5-turbo-0125", tokens);
}

public static I_LLMConfig GROQ_Llama3(int tokens) {
return new GPTConfig(GROQ_COMPLETIONS, "llama-3.1-8b-instant", tokens);
}

public static I_LLM<String> provider(String uri, I_LLMConfig config, String token) {
if (uri == null || uri.isBlank()) {
throw new IllegalArgumentException("llm.uri required");
}
String scheme = uri.split(":", 2)[0].toLowerCase();
for (I_LLMProvider provider : java.util.ServiceLoader.load(I_LLMProvider.class)) {
if (scheme.equals(provider.scheme())) {
return provider.build(config, token);
}
}
throw new IllegalArgumentException("Unsupported LLM scheme: " + scheme);
}

public static I_LLMConfig GROQ_Llama_DeepSeek(int tokens) {
return new GPTConfig(GROQ_COMPLETIONS, "deepseek-r1-distill-llama-70b", tokens);
}

/**
 * Configures an LLM from RDF model statements.
 * Maps RDF triples to GPTConfig fields via reflection injection (Poke).
 * 
 * @param self the resource identifier in the RDF model
 * @param model the RDF model containing configuration
 * @param contextLength the context window size
 * @return configured LLM config
 * @throws SecretsException if RDF injection fails
 */
public static I_LLMConfig configure(Resource self, Model model, int contextLength) 
throws SecretsException {
GPTConfig config = new GPTConfig(contextLength);
try {
Poke.poke(self, model, config);
} catch (Poke.PokeException e) {
throw new SecretsException("Failed to configure LLM from RDF: " + e.getMessage(), e);
}
return config;
}

/**
 * Creates an LLM wrapper with full configuration validation and secret retrieval.
 * 
 * Process:
 * 1. Configure from RDF model
 * 2. Validate all required fields (name, URL)
 * 3. Extract and validate secret name
 * 4. Retrieve API token from secrets manager
 * 5. Create GPT wrapper with validated token
 * 
 * @param self the resource identifier in the RDF model
 * @param model the RDF model containing configuration
 * @param contextLength the context window size
 * @param secrets the secrets provider
 * @return initialized LLM wrapper
 * @throws SecretsException if any validation or configuration step fails
 */
@WithSpan("LLMFactory.llm")
public static GPTWrapper llm(Resource self, Model model, int contextLength, I_Secrets secrets)
throws SecretsException {
I_LLMConfig config = configure(self, model, contextLength);

// Validate configuration completeness
validateLLMConfig(config, self);

// Get and validate secret name
String secretName = getPrettifiedSecretName(config);

// Retrieve and validate token
String token = retrieveAndValidateToken(secretName, secrets, config);

log.info("llm.gpt: {} -> {} @ {} as {}", self, secretName, config.getURL(), config.getResponseFormat());
return new GPTWrapper(token, config);
}

/**
 * Validates that LLM configuration has all required fields.
 * 
 * @param config the configuration to validate
 * @param self the resource identifier for logging
 * @throws SecretsException if configuration is incomplete
 */
private static void validateLLMConfig(I_LLMConfig config, Resource self) throws SecretsException {
if (config == null) {
throw new SecretsException("llm.config.null: " + self);
}
if (config.getName() == null || config.getName().isEmpty()) {
throw new SecretsException("llm.name.missing: " + self);
}
if (config.getURL() == null || config.getURL().isEmpty()) {
throw new SecretsException("llm.url.missing: " + self);
}
}

/**
 * Extracts and validates the secret name from LLM config.
 * 
 * @param config the configuration containing the secret name
 * @return the prettified secret name
 * @throws SecretsException if secret name is missing or invalid
 */
private static String getPrettifiedSecretName(I_LLMConfig config) throws SecretsException {
String rawName = config.getSecretName();
if (rawName == null || rawName.isEmpty()) {
throw new SecretsException("llm.secret.key.missing: " + config.getName());
}
String secretName = PrettyString.localName(rawName);
if (secretName == null || secretName.isEmpty()) {
throw new SecretsException("llm.secret.key.invalid: " + rawName);
}
return secretName;
}

/**
 * Retrieves and validates the API token from secrets manager.
 * Ensures token is not null or whitespace-only.
 * 
 * @param secretName the secret key to retrieve
 * @param secrets the secrets provider
 * @param config the LLM config (for logging)
 * @return the validated API token
 * @throws SecretsException if token is missing or empty
 */
private static String retrieveAndValidateToken(String secretName, I_Secrets secrets, I_LLMConfig config)
throws SecretsException {
String token = secrets.getSecret(secretName);
if (token == null || token.isBlank()) {
throw new SecretsException("llm.secret.missing_or_empty: " + secretName + " for " + config.getName());
}
return token;
}

}
