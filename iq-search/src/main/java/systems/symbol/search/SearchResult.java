package systems.symbol.search;

import java.util.*;

/**
 * Search result container with hits and performance metrics.
 */
public class SearchResult {
    private final List<SearchHit> hits;
    private final SearchStats stats;

    public SearchResult(List<SearchHit> hits, SearchStats stats) {
        this.hits = Collections.unmodifiableList(hits != null ? hits : new ArrayList<>());
        this.stats = stats;
    }

    public List<SearchHit> getHits() { return hits; }
    public SearchStats getStats() { return stats; }
    public boolean isEmpty() { return hits.isEmpty(); }
    public int size() { return hits.size(); }

    /**
     * Get the best matching hit, if any.
     */
    public Optional<SearchHit> best() {
        return hits.isEmpty() ? Optional.empty() : Optional.of(hits.get(0));
    }
}
