package systems.symbol.search;

/**
 * Search performance and context statistics.
 */
public class SearchStats {
    private final String indexType;
    private final long executionTimeMs;
    private final int totalIndexedCount;
    private final int matchedCount;
    private final double avgScore;
    private final long startTime;

    public SearchStats(String indexType, int totalIndexedCount, int matchedCount, 
                      long executionTimeMs, double avgScore) {
        this.indexType = indexType;
        this.totalIndexedCount = totalIndexedCount;
        this.matchedCount = matchedCount;
        this.executionTimeMs = executionTimeMs;
        this.avgScore = avgScore;
        this.startTime = System.currentTimeMillis();
    }

    public String getIndexType() { return indexType; }
    public long getExecutionTimeMs() { return executionTimeMs; }
    public int getTotalIndexedCount() { return totalIndexedCount; }
    public int getMatchedCount() { return matchedCount; }
    public double getAvgScore() { return avgScore; }
    public long getStartTime() { return startTime; }
}
