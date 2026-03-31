package systems.symbol.search.learning;

import systems.symbol.search.SearchHit;
import org.eclipse.rdf4j.model.IRI;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for LLM-based ranking strategies.
 * Implementations score search results for relevance.
 */
public interface LLMRanker {
    
    /**
     * Score a single result for relevance to the query in concept context.
     * 
     * @param query The search query
     * @param concept The semantic concept context
     * @param result The search result to score
     * @return Relevance score [0-1], where 1.0 = perfectly relevant
     */
    double scoreResult(String query, IRI concept, SearchHit result);
    
    /**
     * Score multiple results in parallel.
     * 
     * @param query The search query
     * @param concept The semantic concept context
     * @param results Results to score
     * @return List of scores parallel to results list
     */
    List<Double> scoreResults(String query, IRI concept, List<SearchHit> results);
    
    /**
     * Asynchronous version for non-blocking scoring.
     */
    CompletableFuture<List<Double>> scoreResultsAsync(
        String query, IRI concept, List<SearchHit> results);
    
    /**
     * Get human-readable reason for the score.
     * Used for explainability and debugging.
     */
    String explainScore(String query, IRI concept, SearchHit result);
    
    /**
     * Clear internal caches (if any).
     */
    void clearCache();
    
    /**
     * Get ranker type identifier.
     */
    String getType();
}
