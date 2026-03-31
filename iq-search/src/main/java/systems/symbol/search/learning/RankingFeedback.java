package systems.symbol.search.learning;

import org.eclipse.rdf4j.model.IRI;
import java.time.Instant;

/**
 * Feedback data point for ranking model training.
 * Captures user relevance judgments for query-result pairs.
 */
public class RankingFeedback {
    private final String query;
    private final IRI concept;
    private final IRI resultEntity;
    private final int relevanceScore;  // 0-5: not relevant to very relevant
    private final int position;  // Position in original ranking (for LTR loss calculation)
    private final Instant timestamp;
    private final String userSessionId;
    private final double initialScore;  // Score before L2R
    
    private RankingFeedback(Builder builder) {
        this.query = builder.query;
        this.concept = builder.concept;
        this.resultEntity = builder.resultEntity;
        this.relevanceScore = builder.relevanceScore;
        this.position = builder.position;
        this.timestamp = builder.timestamp;
        this.userSessionId = builder.userSessionId;
        this.initialScore = builder.initialScore;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public String getQuery() {
        return query;
    }
    
    public IRI getConcept() {
        return concept;
    }
    
    public IRI getResultEntity() {
        return resultEntity;
    }
    
    public int getRelevanceScore() {
        return relevanceScore;
    }
    
    public int getPosition() {
        return position;
    }
    
    public Instant getTimestamp() {
        return timestamp;
    }
    
    public String getUserSessionId() {
        return userSessionId;
    }
    
    public double getInitialScore() {
        return initialScore;
    }
    
    /**
     * Convert feedback score to normalized relevance (0-1).
     * 0-2: not relevant (0.0)
     * 3: somewhat relevant (0.5)
     * 4-5: very relevant (1.0)
     */
    public double getNormalizedRelevance() {
        if (relevanceScore <= 2) return 0.0;
        if (relevanceScore == 3) return 0.5;
        return 1.0;
    }
    
    /**
     * Calculate pairwise loss for this feedback vs another.
     * Used for LambdaMART-style training.
     */
    public double pairwiseLoss(RankingFeedback other) {
        double delta = Math.abs(this.getNormalizedRelevance() - other.getNormalizedRelevance());
        int positionDelta = Math.abs(this.position - other.position);
        // Loss increases if less relevant result ranks higher
        return delta * Math.log(1.0 + positionDelta);
    }
    
    @Override
    public String toString() {
        return "RankingFeedback{" +
                "query='" + query + '\'' +
                ", concept=" + concept +
                ", resultEntity=" + resultEntity +
                ", relevanceScore=" + relevanceScore +
                ", position=" + position +
                ", timestamp=" + timestamp +
                '}';
    }
    
    // Builder
    public static class Builder {
        private String query;
        private IRI concept;
        private IRI resultEntity;
        private int relevanceScore = 3;  // Default: somewhat relevant
        private int position = 0;
        private Instant timestamp = Instant.now();
        private String userSessionId;
        private double initialScore = 0.5;
        
        public Builder query(String query) {
            this.query = query;
            return this;
        }
        
        public Builder concept(IRI concept) {
            this.concept = concept;
            return this;
        }
        
        public Builder resultEntity(IRI resultEntity) {
            this.resultEntity = resultEntity;
            return this;
        }
        
        public Builder relevanceScore(int score) {
            if (score < 0 || score > 5) {
                throw new IllegalArgumentException("Relevance score must be 0-5");
            }
            this.relevanceScore = score;
            return this;
        }
        
        public Builder position(int position) {
            this.position = position;
            return this;
        }
        
        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }
        
        public Builder userSessionId(String sessionId) {
            this.userSessionId = sessionId;
            return this;
        }
        
        public Builder initialScore(double score) {
            this.initialScore = score;
            return this;
        }
        
        public RankingFeedback build() {
            return new RankingFeedback(this);
        }
    }
}
