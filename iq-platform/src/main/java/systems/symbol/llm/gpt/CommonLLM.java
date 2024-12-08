package systems.symbol.llm.gpt;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import systems.symbol.llm.I_LLM;
import systems.symbol.llm.I_LLMConfig;
import systems.symbol.platform.Poke;
import systems.symbol.rdf4j.io.RDFDump;
import systems.symbol.secrets.I_Secrets;
import systems.symbol.secrets.SecretsException;
import systems.symbol.string.PrettyString;

public class CommonLLM {
    static protected final Logger log = LoggerFactory.getLogger(CommonLLM.class);

    public static final String OPENAI_COMPLETIONS = "https://api.openai.com/v1/chat/completions";
    public static final String GROQ_COMPLETIONS = "https://api.groq.com/openai/v1/chat/completions";

    public static I_LLMConfig GPT3_5_Turbo(int tokens) {
        return new GPTConfig(OPENAI_COMPLETIONS, "gpt-3.5-turbo-0125", tokens);
    }

    public static I_LLMConfig GROQ_Llama3_7b(int tokens) {
        return new GPTConfig(GROQ_COMPLETIONS, "llama3-8b-8192", tokens);
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

    public static I_LLM<String> complete(Resource self, Model model, int contextLength, I_Secrets secrets)
            throws SecretsException {
        I_LLMConfig config = configure(self, model, contextLength);
        if (config.getName() == null || config.getURL() == null) {
            log.error("llm.config.missing: {} -> {} -> {}", self, config.getName(), config.getURL());
            log.error("llm.config.dump: {} -> {}", model.size(), model.getStatements(self, null, null));
            try {
                RDFDump.dump(model, new FileOutputStream(new java.io.File("debug.ttl")), RDFFormat.TURTLE);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
        String secretName = PrettyString.localName(config.getSecretName());
        log.info("llm.complete: {} -> {} @ {} as {}", self, secretName, config.getURL(), config.getResponseFormat());
        if (secretName == null || secretName.isEmpty())
            throw new SecretsException("secret.missing: " + config.getName());
        String token = secrets.getSecret(secretName);
        if (token == null)
            throw new SecretsException("missing: " + secretName);
        return new GenericGPT(token, config);
    }
}
