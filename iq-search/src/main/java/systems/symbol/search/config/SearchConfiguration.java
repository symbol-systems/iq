package systems.symbol.search.config;

import java.util.HashMap;
import java.util.Map;

/**
 * Search configuration - typically loaded from .iq/config.yaml
 * or system properties.
 */
public class SearchConfiguration {
    
    public enum IndexType {
        VECTOR, BM25, HYBRID, GRAPH
    }

    private IndexType indexType = IndexType.HYBRID;
    private boolean persistenceEnabled = true;
    private String persistencePath = ".iq/indexes";
    
    // Vector config
    private String embeddingModel = "all-MiniLM-L6-v2";
    private int embeddingDimension = 384;
    
    // BM25 config
    private float bm25K1 = 1.2f;
    private float bm25B = 0.75f;
    
    // Hybrid config
    private double vectorWeight = 0.6;
    private double bm25Weight = 0.4;
    
    // Graph config
    private boolean graphSearchEnabled = false;
    private double graphBoost = 0.3;
    private int maxGraphPaths = 50;
    
    // Performance config
    private int queryTimeoutMs = 5000;
    private int maxResultsPerIndex = 1000;
    private int cacheSize = 256;  // MB
    
    // Field boost config
    private final Map<String, Float> fieldBoosts = new HashMap<>();
    
    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }

    public IndexType getIndexType() { return indexType; }
    public boolean isPersistenceEnabled() { return persistenceEnabled; }
    public String getPersistencePath() { return persistencePath; }
    public String getEmbeddingModel() { return embeddingModel; }
    public int getEmbeddingDimension() { return embeddingDimension; }
    public float getBm25K1() { return bm25K1; }
    public float getBm25B() { return bm25B; }
    public double getVectorWeight() { return vectorWeight; }
    public double getBm25Weight() { return bm25Weight; }
    public boolean isGraphSearchEnabled() { return graphSearchEnabled; }
    public double getGraphBoost() { return graphBoost; }
    public int getMaxGraphPaths() { return maxGraphPaths; }
    public int getQueryTimeoutMs() { return queryTimeoutMs; }
    public int getMaxResultsPerIndex() { return maxResultsPerIndex; }
    public int getCacheSize() { return cacheSize; }
    public Map<String, Float> getFieldBoosts() { return fieldBoosts; }

    public static class Builder {
        private final SearchConfiguration config = new SearchConfiguration();

        public Builder indexType(IndexType t) { config.indexType = t; return this; }
        public Builder persistenceEnabled(boolean p) { config.persistenceEnabled = p; return this; }
        public Builder persistencePath(String p) { config.persistencePath = p; return this; }
        public Builder embeddingModel(String m) { config.embeddingModel = m; return this; }
        public Builder embeddingDimension(int d) { config.embeddingDimension = d; return this; }
        public Builder bm25K1(float k) { config.bm25K1 = k; return this; }
        public Builder bm25B(float b) { config.bm25B = b; return this; }
        public Builder vectorWeight(double w) { config.vectorWeight = w; return this; }
        public Builder bm25Weight(double w) { config.bm25Weight = w; return this; }
        public Builder graphSearchEnabled(boolean g) { config.graphSearchEnabled = g; return this; }
        public Builder graphBoost(double b) { config.graphBoost = b; return this; }
        public Builder maxGraphPaths(int m) { config.maxGraphPaths = m; return this; }
        public Builder queryTimeoutMs(int t) { config.queryTimeoutMs = t; return this; }
        public Builder maxResultsPerIndex(int m) { config.maxResultsPerIndex = m; return this; }
        public Builder cacheSize(int c) { config.cacheSize = c; return this; }
        public Builder fieldBoost(String field, float boost) {
            config.fieldBoosts.put(field, boost);
            return this;
        }

        public SearchConfiguration build() {
            return config;
        }
    }
}
