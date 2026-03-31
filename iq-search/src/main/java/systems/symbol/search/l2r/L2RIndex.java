package systems.symbol.search.l2r;

import systems.symbol.search.*;
import systems.symbol.search.learning.LLMRanker;
import systems.symbol.search.learning.RankingFeedback;
import org.eclipse.rdf4j.model.IRI;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Learning-to-Rank index that wraps any I_Index with LLM-based reranking.
 * 
 * Architecture:
 * 1. Wrapped index returns initial search results
 * 2. L2RIndex scores each result using LLMRanker
 * 3. Results reranked by combined score: (0.7 * initial) + (0.3 * llm_score)
 * 4. Feedback collected for offline training
 * 
 * Example:
 * <pre>
 * I_Index baseIndex = IndexFactory.hybrid(0.6);
 * LLMRanker ranker = new PromptBasedRanker(llmClient);
 * I_Index l2rIndex = new L2RIndex(baseIndex, ranker, 0.3);
 * 
 * SearchResult result = l2rIndex.search(request);
 * // Results auto-reranked by LLM relevance
 * </pre>
 */
public class L2RIndex implements I_Index {
    
    private final I_Index wrappedIndex;
    private final LLMRanker ranker;
    private final double l2rWeight;  // 0.0-1.0, weight for LLM score in final ranking
    private final List<RankingFeedback> feedbackBuffer;
    private final int maxFeedbackBufferSize;
    private boolean feedbackCollectionEnabled = true;
    
    public L2RIndex(I_Index wrappedIndex, LLMRanker ranker) {
        this(wrappedIndex, ranker, 0.3);
    }
    
    public L2RIndex(I_Index wrappedIndex, LLMRanker ranker, double l2rWeight) {
        this.wrappedIndex = wrappedIndex;
        this.ranker = ranker;
        this.l2rWeight = Math.max(0.0, Math.min(1.0, l2rWeight));  // Clamp to [0, 1]
        this.feedbackBuffer = Collections.synchronizedList(new ArrayList<>());
        this.maxFeedbackBufferSize = 10000;
    }
    
    @Override
    public void index(IRI entity, String text, IRI concept) {
        wrappedIndex.index(entity, text, concept);
    }
    
    @Override
    public SearchResult search(SearchRequest request) {
        long startTime = System.currentTimeMillis();
        
        // Step 1: Get base results from wrapped index
        SearchResult baseResult = wrappedIndex.search(request);
        
        if (baseResult.isEmpty()) {
            return baseResult;  // No results to rerank
        }
        
        // Step 2: Score each result with LLM ranker
        List<SearchHit> baseHits = baseResult.getHits();
        List<Double> llmScores = ranker.scoreResults(
            request.getQuery(),
            request.getConcept().orElse(null),
            baseHits
        );
        
        // Step 3: Combine scores and rerank
        List<RankedHit> rankedHits = new ArrayList<>();
        for (int i = 0; i < baseHits.size(); i++) {
            SearchHit hit = baseHits.get(i);
            double llmScore = llmScores.get(i);
            double combinedScore = combineScores(hit.score(), llmScore);
            
            rankedHits.add(new RankedHit(hit, llmScore, combinedScore, i));
        }
        
        // Step 4: Sort by combined score
        rankedHits.sort(Comparator.comparingDouble(RankedHit::getCombiledScore).reversed());
        
        // Step 5: Create new hits with reranked scores
        List<SearchHit> rerankedHits = rankedHits.stream()
            .map(rh -> new SearchHit(
                rh.hit.intent(),
                rh.combinedScore,
                rh.hit.getMatchedText(),
                enrichMetadata(rh.hit.getMetadata(), rh)
            ))
            .collect(Collectors.toList());
        
        long executionTime = System.currentTimeMillis() - startTime;
        
        // Step 6: Create new result with updated stats
        SearchStats rerankedStats = new SearchStats(
            "l2r-" + baseResult.getStats().getIndexType(),
            executionTime + 100,  // Add LLM ranking time estimate
            baseResult.getStats().getTotalIndexedCount(),
            rerankedHits.size(),
            rerankedHits.isEmpty() ? 0 : 
                rerankedHits.stream().mapToDouble(SearchHit::score).average().orElse(0)
        );
        
        return new SearchResult(rerankedHits, rerankedStats);
    }
    
    @Override
    public void clear() {
        wrappedIndex.clear();
        feedbackBuffer.clear();
        ranker.clearCache();
    }
    
    @Override
    public String getType() {
        return "l2r";
    }
    
    /**
     * Record feedback for training.
     * Accumulates feedback for offline model training and improvement.
     */
    public void recordFeedback(RankingFeedback feedback) {
        if (!feedbackCollectionEnabled) return;
        
        feedbackBuffer.add(feedback);
        
        // Maintain buffer size
        if (feedbackBuffer.size() > maxFeedbackBufferSize) {
            feedbackBuffer.remove(0);
        }
    }
    
    /**
     * Get accumulated feedback for training.
     */
    public List<RankingFeedback> getFeedback() {
        return new ArrayList<>(feedbackBuffer);
    }
    
    /**
     * Clear feedback buffer.
     */
    public void clearFeedback() {
        feedbackBuffer.clear();
    }
    
    /**
     * Enable/disable feedback collection.
     */
    public void setFeedbackCollectionEnabled(boolean enabled) {
        this.feedbackCollectionEnabled = enabled;
    }
    
    /**
     * Get explanation for a result's L2R score.
     */
    public String explainResult(SearchRequest request, SearchHit result) {
        return ranker.explainScore(
            request.getQuery(),
            request.getConcept().orElse(null),
            result
        );
    }
    
    /**
     * Combine base search score with LLM ranking score.
     * Formula: (1 - l2rWeight) * baseScore + l2rWeight * llmScore
     */
    private double combineScores(double baseScore, double llmScore) {
        return (1.0 - l2rWeight) * baseScore + l2rWeight * llmScore;
    }
    
    /**
     * Enrich hit metadata with L2R information.
     */
    private Map<String, Object> enrichMetadata(Map<String, Object> original, RankedHit ranked) {
        Map<String, Object> enriched = new HashMap<>(original);
        enriched.put("llm_score", ranked.llmScore);
        enriched.put("original_position", ranked.originalPosition);
        enriched.put("reranked_position", 0);  // Will be updated externally
        enriched.put("l2r_explanation", "");  // Populated on demand
        return enriched;
    }
    
    /**
     * Internal class for ranking calculations.
     */
    private static class RankedHit {
        SearchHit hit;
        double llmScore;
        double combinedScore;
        int originalPosition;
        
        RankedHit(SearchHit hit, double llmScore, double combinedScore, int originalPosition) {
            this.hit = hit;
            this.llmScore = llmScore;
            this.combinedScore = combinedScore;
            this.originalPosition = originalPosition;
        }
        
        double getCombiledScore() {
            return combinedScore;
        }
    }
}
