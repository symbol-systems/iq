package systems.symbol.platform.search;

import systems.symbol.search.*;
import systems.symbol.search.l2r.L2RIndex;
import systems.symbol.search.learning.LLMRanker;
import systems.symbol.search.learning.PromptBasedRanker;
import systems.symbol.llm.gpt.GPTWrapper;
import org.eclipse.rdf4j.model.IRI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for creating configured search indexes integrated with iq-platform.
 * 
 * Bridges IQ search infrastructure with existing GPTWrapper and realm configuration.
 * 
 * Example:
 * <pre>
 * GPTWrapper gpt = LLMFactory.llm(...);
 * IQSearchFactory factory = new IQSearchFactory(gpt);
 * 
 * I_Index hybridSearch = factory.hybrid(0.6);
 * I_Index l2rSearch = factory.withL2R(hybridSearch, 0.3);
 * </pre>
 */
public class IQSearchFactory {
    private static final Logger log = LoggerFactory.getLogger(IQSearchFactory.class);
    public static final String DEFAULT_PROVIDER = "openai"; // Default LLM provider for L2R
    
    private final GPTWrapper gptWrapper;
    
    public IQSearchFactory(GPTWrapper gptWrapper) {
        this.gptWrapper = gptWrapper;
        log.info("search.factory: initialized with {}", gptWrapper);
    }
    
    /**
     * Create vector-only search.
     */
    public I_Index vector() {
        return IndexFactory.vector();
    }
    
    /**
     * Create BM25 full-text search.
     */
    public I_Index bm25() {
        return IndexFactory.bm25();
    }
    
    /**
     * Create hybrid vector + BM25 search.
     * 
     * @param vectorWeight Weight for vector scores [0-1], default 0.6
     */
    public I_Index hybrid(double vectorWeight) {
        return IndexFactory.hybrid(vectorWeight);
    }
    
    /**
     * Wrap index with L2R (LLM-based learning-to-rank).
     * 
     * @param baseIndex The base search index to wrap
     * @param l2rWeight Weight for LLM scores [0-1], default 0.3
     */
    public I_Index withL2R(I_Index baseIndex, double l2rWeight) {
        // Create LLM client adapter
        PromptBasedRanker.LLMClient llmClient = new GPTWrapperLLMClient(gptWrapper);
        
        // Create ranker with caching
        LLMRanker ranker = new PromptBasedRanker(llmClient, 1000);
        
        // Wrap base index with L2R
        I_Index l2rIndex = new L2RIndex(baseIndex, ranker, l2rWeight);
        
        log.info("search.l2r: wrapped {} with weight {}", baseIndex.getType(), l2rWeight);
        return l2rIndex;
    }
    
    /**
     * Full hybrid + L2R search (recommended).
     * 60% vector, 40% BM25, reranked with 30% LLM score.
     */
    public I_Index fullSearch() {
        I_Index hybrid = hybrid(0.6);
        return withL2R(hybrid, 0.3);
    }
    
    /**
     * Get the wrapped GPTWrapper.
     */
    public GPTWrapper getGPTWrapper() {
        return gptWrapper;
    }
}
