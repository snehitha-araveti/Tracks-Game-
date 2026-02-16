package Tracks;

/* Dynamic Programming performance metrics */
public class DPMetrics {
    public long executionTimeMs;
    public int statesExplored;
    public int cacheHits;
    public int cacheMisses;
    public int optimalMoves;
    public int recursionDepth;
    
    public DPMetrics() {
        this.executionTimeMs = 0;
        this.statesExplored = 0;
        this.cacheHits = 0;
        this.cacheMisses = 0;
        this.optimalMoves = 0;
        this.recursionDepth = 0;
    }
    
    public void reset() {
        executionTimeMs = 0;
        statesExplored = 0;
        cacheHits = 0;
        cacheMisses = 0;
        optimalMoves = 0;
        recursionDepth = 0;
    }
    
    @Override
    public String toString() {
        return String.format(
            "DP Performance Metrics:\n" +
            "  Execution Time: %d ms\n" +
            "  States Explored: %d\n" +
            "  Cache Hits: %d (%.1f%%)\n" +
            "  Cache Misses: %d\n" +
            "  Optimal Moves Found: %d\n" +
            "  Max Recursion Depth: %d",
            executionTimeMs, 
            statesExplored, 
            cacheHits, 
            statesExplored > 0 ? (100.0 * cacheHits / statesExplored) : 0,
            cacheMisses,
            optimalMoves,
            recursionDepth
        );
    }
}
