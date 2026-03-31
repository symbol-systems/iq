package systems.symbol.search.hybrid;

import systems.symbol.search.*;
import systems.symbol.search.vector.VectorIndex;
import systems.symbol.search.bm25.BM25Index;
import org.eclipse.rdf4j.model.IRI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Hybrid search combining BM25 and Vector similarity.
 * Phase 2: Linear interpolation of normalized scores.
 * Benefits: Exact keyword matches + semantic relevance
 */
public class HybridIndex implements I_Index {
    private static final Logger log = LoggerFactory.getLogger(HybridIndex.class);
    
    private final VectorIndex vectorIndex;
    private final BM25Index bm25Index;
    private final double vectorWeight;
    private final double bm25Weight;

    public HybridIndex(double vectorWeight) {
        this(new VectorIndex(), new BM25Index(), vectorWeight, 1.0 - vectorWeight);
    }

    public HybridIndex(VectorIndex vectorIndex, BM25Index bm25Index, 
                      double vectorWeight, double bm25Weight) {
        this.vectorIndex = vectorIndex;
        this.bm25Index = bm25Index;
        this.vectorWeight = vectorWeight;
        this.bm25Weight = bm25Weight;
        
        // Normalize weights
        double total = vectorWeight + bm25Weight;
        this.vectorWeight /= total;
        this.bm25Weight /= total;
    }

    @Override
    public void index(IRI entity, String text, IRI concept) {
        vectorIndex.index(entity, text, concept);
        bm25Index.index(entity, text, concept);
        log.debug("hybrid.index: {} (vector+bm25)", entity);
    }

    @Override
    public SearchResult search(SearchRequest request) {
        long startTime = System.currentTimeMillis();
        
        // Run both indexes
        SearchResult vectorResult = vectorIndex.search(request);
        SearchResult bm25Result = bm25Index.search(request);
        
        // Merge results with weighted scoring
        Map<IRI, Double> mergedScores = new HashMap<>();
        Map<IRI, SearchHit> bestHits = new HashMap<>();
        
        // Add vector hits
        for (SearchHit hit : vectorResult.getHits()) {
            double score = hit.score() * vectorWeight;
            mergedScores.put(hit.intent(), score);
            bestHits.put(hit.intent(), hit);
        }
        
        // Add/merge BM25 hits
        for (SearchHit hit : bm25Result.getHits()) {
            double bm25Score = hit.score() * bm25Weight;
            IRI iri = hit.intent();
            
            if (mergedScores.containsKey(iri)) {
                // Average both scores
                double combinedScore = (mergedScores.get(iri) + bm25Score) / 2.0;
                mergedScores.put(iri, combinedScore);
            } else {
                mergedScores.put(iri, bm25Score);
                bestHits.put(iri, hit);
            }
        }
        
        // Convert to sorted list
        List<SearchHit> combined = new ArrayList<>();
        for (Map.Entry<IRI, Double> entry : mergedScores.entrySet()) {
            double score = entry.getValue();
            if (score >= request.getMinScore()) {
                SearchHit original = bestHits.get(entry.getKey());
                combined.add(new SearchHit(entry.getKey(), score, 
                    original != null ? original.getMatchedText() : null));
            }
        }
        
        combined.sort((a, b) -> Double.compare(b.score(), a.score()));
        List<SearchHit> topK = combined.size() > request.getMaxResults()
            ? combined.subList(0, request.getMaxResults())
            : combined;
        
        long elapsed = System.currentTimeMillis() - startTime;
        double avgScore = topK.isEmpty() ? 0.0 
            : topK.stream().mapToDouble(SearchHit::score).average().orElse(0.0);
        
        log.info("hybrid.search: {} results (vector:{}, bm25:{}, weights: v={}, b={})",
            topK.size(), vectorResult.size(), bm25Result.size(), vectorWeight, bm25Weight);
        
        return new SearchResult(topK, 
            new SearchStats("hybrid", vectorIndex.getType().equals("vector") ? 
                vectorResult.getStats().getTotalIndexedCount() : 0,
                topK.size(), elapsed, avgScore));
    }

    @Override
    public void clear() {
        vectorIndex.clear();
        bm25Index.clear();
    }

    @Override
    public String getType() {
        return "hybrid";
    }
}
