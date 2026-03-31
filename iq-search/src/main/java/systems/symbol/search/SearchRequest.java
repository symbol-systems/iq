package systems.symbol.search;

import org.eclipse.rdf4j.model.IRI;

/**
 * Search request builder with fluent API.
 */
public class SearchRequest {
    private final String query;
    private final IRI concept;
    private final int maxResults;
    private final double minScore;
    private final long timeoutMs;
    private final Map<String, Object> filters;

    private SearchRequest(Builder builder) {
        this.query = builder.query;
        this.concept = builder.concept;
        this.maxResults = builder.maxResults;
        this.minScore = builder.minScore;
        this.timeoutMs = builder.timeoutMs;
        this.filters = Collections.unmodifiableMap(builder.filters);
    }

    public String getQuery() { return query; }
    public IRI getConcept() { return concept; }
    public int getMaxResults() { return maxResults; }
    public double getMinScore() { return minScore; }
    public long getTimeoutMs() { return timeoutMs; }
    public Map<String, Object> getFilters() { return filters; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String query;
        private IRI concept;
        private int maxResults = 20;
        private double minScore = 0.0;
        private long timeoutMs = 5000;
        private final Map<String, Object> filters = new HashMap<>();

        public Builder query(String q) { this.query = q; return this; }
        public Builder concept(IRI c) { this.concept = c; return this; }
        public Builder maxResults(int m) { this.maxResults = m; return this; }
        public Builder minScore(double m) { this.minScore = m; return this; }
        public Builder timeoutMs(long t) { this.timeoutMs = t; return this; }
        public Builder filter(String key, Object value) { 
            this.filters.put(key, value); 
            return this; 
        }

        public SearchRequest build() {
            if (query == null) query = "";
            return new SearchRequest(this);
        }
    }
}
