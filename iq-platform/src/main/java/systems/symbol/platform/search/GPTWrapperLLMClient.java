package systems.symbol.platform.search;

import systems.symbol.search.learning.PromptBasedRanker;
import systems.symbol.llm.gpt.GPTWrapper;
import systems.symbol.tools.APIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;

/**
 * LLM client adapter bridging PromptBasedRanker with existing GPTWrapper.
 * 
 * Allows L2R ranking to use the same LLM infrastructure as the rest of IQ.
 * 
 * Example:
 * <pre>
 * GPTWrapper gptWrapper = ...; // From iq-platform
 * PromptBasedRanker.LLMClient client = new GPTWrapperLLMClient(gptWrapper);
 * LLMRanker ranker = new PromptBasedRanker(client);
 * </pre>
 */
public class GPTWrapperLLMClient implements PromptBasedRanker.LLMClient {
    private static final Logger log = LoggerFactory.getLogger(GPTWrapperLLMClient.class);
    private final GPTWrapper gptWrapper;
    private static final int PROMPT_MAX_TOKENS = 200;  // For ranking prompts
    
    public GPTWrapperLLMClient(GPTWrapper gptWrapper) {
        this.gptWrapper = gptWrapper;
        log.info("search.l2r.client: initialized with {}", gptWrapper);
    }
    
    @Override
    public String generate(String prompt) throws Exception {
        try {
            // Use existing GPTWrapper conversation API
            systems.symbol.llm.Conversation chat = new systems.symbol.llm.Conversation();
            chat.user(prompt);
            
            // Call LLM
            gptWrapper.complete(chat);
            
            // Extract response
            String response = chat.latest().getContent();
            if (response == null || response.isEmpty()) {
                log.warn("search.l2r.empty_response: prompt length={}", prompt.length());
                return "0.5";  // Default score on empty response
            }
            
            log.debug("search.l2r.response: {} chars", response.length());
            return response;
        } catch (APIException | IOException e) {
            log.error("search.l2r.error: {}", e.getMessage());
            throw new Exception("LLM ranking failed: " + e.getMessage(), e);
        }
    }
}
