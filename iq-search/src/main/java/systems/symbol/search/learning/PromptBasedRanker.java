package systems.symbol.search.learning;

import systems.symbol.search.SearchHit;
import org.eclipse.rdf4j.model.IRI;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * LLM-based ranker using prompt-based relevance scoring.
 * Evaluates search results using a structured relevance assessment prompt.
 */
public class PromptBasedRanker implements LLMRanker {
    
    private final LLMClient llmClient;
    private final Map<String, Double> scoreCache;  // Cache for frequently scored results
    private final int maxCacheSize;
    private static final int DEFAULT_CACHE_SIZE = 1000;
    
    public PromptBasedRanker(LLMClient llmClient) {
        this(llmClient, DEFAULT_CACHE_SIZE);
    }
    
    public PromptBasedRanker(LLMClient llmClient, int maxCacheSize) {
        this.llmClient = llmClient;
        this.maxCacheSize = maxCacheSize;
        this.scoreCache = Collections.synchronizedMap(new LinkedHashMap<String, Double>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry eldest) {
                return size() > maxCacheSize;
            }
        });
    }
    
    @Override
    public double scoreResult(String query, IRI concept, SearchHit result) {
        String cacheKey = buildCacheKey(query, concept, result);
        
        // Check cache
        if (scoreCache.containsKey(cacheKey)) {
            return scoreCache.get(cacheKey);
        }
        
        // Construct ranking prompt
        String prompt = buildRankingPrompt(query, concept, result);
        
        try {
            // Call LLM to score
            String response = llmClient.generate(prompt);
            double score = extractScoreFromResponse(response);
            
            // Cache result
            scoreCache.put(cacheKey, score);
            return score;
        } catch (Exception e) {
            // Fall back to initial search score on error
            System.err.println("Error scoring result: " + e.getMessage());
            return result.getScore();
        }
    }
    
    @Override
    public List<Double> scoreResults(String query, IRI concept, List<SearchHit> results) {
        return results.stream()
            .map(hit -> scoreResult(query, concept, hit))
            .collect(Collectors.toList());
    }
    
    @Override
    public CompletableFuture<List<Double>> scoreResultsAsync(
        String query, IRI concept, List<SearchHit> results) {
        
        return CompletableFuture.supplyAsync(() -> 
            scoreResults(query, concept, results)
        );
    }
    
    @Override
    public String explainScore(String query, IRI concept, SearchHit result) {
        String prompt = buildExplanationPrompt(query, concept, result);
        try {
            return llmClient.generate(prompt);
        } catch (Exception e) {
            return "Unable to generate explanation: " + e.getMessage();
        }
    }
    
    @Override
    public void clearCache() {
        scoreCache.clear();
    }
    
    @Override
    public String getType() {
        return "prompt-based";
    }
    
    /**
     * Build prompt for relevance scoring.
     */
    private String buildRankingPrompt(String query, IRI concept, SearchHit result) {
        return String.format(
            "You are a search relevance evaluator.\n" +
            "\n" +
            "Query: %s\n" +
            "Concept: %s\n" +
            "Search Result: %s (%s)\n" +
            "Matched Text: %s\n" +
            "Initial Score: %.2f\n" +
            "\n" +
            "Rate the relevance of this result on a scale of 0.0 to 1.0, where:\n" +
            "- 0.0 = completely irrelevant\n" +
            "- 0.5 = somewhat relevant but missing key aspects\n" +
            "- 1.0 = perfectly relevant to the query and concept\n" +
            "\n" +
            "Consider semantic meaning, contextual fit, and practical usefulness.\n" +
            "Respond with ONLY a decimal number between 0.0 and 1.0, e.g., 0.85\n",
            query,
            concept.stringValue(),
            result.intent().stringValue(),
            getEntityLabel(result.intent()),
            result.getMatchedText(),
            result.getScore()
        );
    }
    
    /**
     * Build prompt for explanation.
     */
    private String buildExplanationPrompt(String query, IRI concept, SearchHit result) {
        return String.format(
            "You are a search result analyzer. Provide a concise (1-2 sentences) explanation " +
            "for why this result is relevant to the query.\n\n" +
            "Query: %s\n" +
            "Concept: %s\n" +
            "Result: %s\n" +
            "Matched Text: %s\n" +
            "Initial Score: %.2f\n\n" +
            "Explain the relevance concisely.\n",
            query,
            concept.stringValue(),
            result.intent().stringValue(),
            result.getMatchedText(),
            result.getScore()
        );
    }
    
    /**
     * Extract score from LLM response.
     * Expects a decimal number between 0.0 and 1.0.
     */
    private double extractScoreFromResponse(String response) {
        try {
            // Try to find decimal in response
            String cleaned = response.trim();
            
            // If response looks like "0.85" or "0.85\n" or "0.85 some text"
            String[] parts = cleaned.split("\\s+");
            for (String part : parts) {
                try {
                    double value = Double.parseDouble(part);
                    if (value >= 0.0 && value <= 1.0) {
                        return value;
                    }
                } catch (NumberFormatException ignored) {
                    // Continue to next token
                }
            }
            
            // If no valid score found, return middle estimate
            System.err.println("Could not extract score from: " + response);
            return 0.5;
        } catch (Exception e) {
            System.err.println("Error parsing score: " + e.getMessage());
            return 0.5;
        }
    }
    
    /**
     * Extract entity label from IRI.
     */
    private String getEntityLabel(IRI iri) {
        String str = iri.stringValue();
        int lastSlash = str.lastIndexOf('/');
        int lastHash = str.lastIndexOf('#');
        int idx = Math.max(lastSlash, lastHash);
        return idx >= 0 ? str.substring(idx + 1) : str;
    }
    
    /**
     * Build cache key for result.
     */
    private String buildCacheKey(String query, IRI concept, SearchHit result) {
        return query + "|" + concept.stringValue() + "|" + result.intent().stringValue();
    }
    
    /**
     * LLM client interface for dependency injection.
     */
    public interface LLMClient {
        String generate(String prompt) throws Exception;
    }
}
