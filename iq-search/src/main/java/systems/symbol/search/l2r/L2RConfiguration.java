package systems.symbol.search.l2r;

import systems.symbol.search.learning.LLMRanker;

/**
 * Configuration for Learning-to-Rank indices.
 */
public class L2RConfiguration {
    private final double l2rWeight;
    private final String rankerType;
    private final int cacheSize;
    private final long timeoutMs;
    private final boolean explainabilityEnabled;
    private final boolean feedbackCollectionEnabled;
    
    private L2RConfiguration(Builder builder) {
        this.l2rWeight = builder.l2rWeight;
        this.rankerType = builder.rankerType;
        this.cacheSize = builder.cacheSize;
        this.timeoutMs = builder.timeoutMs;
        this.explainabilityEnabled = builder.explainabilityEnabled;
        this.feedbackCollectionEnabled = builder.feedbackCollectionEnabled;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public double getL2rWeight() {
        return l2rWeight;
    }
    
    public String getRankerType() {
        return rankerType;
    }
    
    public int getCacheSize() {
        return cacheSize;
    }
    
    public long getTimeoutMs() {
        return timeoutMs;
    }
    
    public boolean isExplainabilityEnabled() {
        return explainabilityEnabled;
    }
    
    public boolean isFeedbackCollectionEnabled() {
        return feedbackCollectionEnabled;
    }
    
    public static class Builder {
        private double l2rWeight = 0.3;
        private String rankerType = "prompt-based";
        private int cacheSize = 1000;
        private long timeoutMs = 5000;
        private boolean explainabilityEnabled = true;
        private boolean feedbackCollectionEnabled = true;
        
        public Builder l2rWeight(double weight) {
            this.l2rWeight = Math.max(0.0, Math.min(1.0, weight));
            return this;
        }
        
        public Builder rankerType(String type) {
            this.rankerType = type;
            return this;
        }
        
        public Builder cacheSize(int size) {
            this.cacheSize = size;
            return this;
        }
        
        public Builder timeoutMs(long ms) {
            this.timeoutMs = ms;
            return this;
        }
        
        public Builder explainabilityEnabled(boolean enabled) {
            this.explainabilityEnabled = enabled;
            return this;
        }
        
        public Builder feedbackCollectionEnabled(boolean enabled) {
            this.feedbackCollectionEnabled = enabled;
            return this;
        }
        
        public L2RConfiguration build() {
            return new L2RConfiguration(this);
        }
    }
}
