package Tracks;

/**
 * Records time and space metrics for a single algorithm step.
 * Used to build the final analysis report.
 */
public class AlgoMetrics {
	/** Name of the algorithm (Greedy, DC, DP, Backtracking) */
	public String algoName;

	/** Time for the current step in nanoseconds */
	public long stepTimeNs;

	/** Number of operations in current step */
	public int opsCount;

	/** Auxiliary space used (grid cells or table size) */
	public int spaceUsed;

	/** Total moves made by the computer so far */
	public int totalMoves;

	/** Cumulative algorithm time in nanoseconds */
	public long totalTimeNs;

	/** Time complexity label (e.g., O(N²)) */
	public String timeComplexity;

	/** Space complexity label (e.g., O(N)) */
	public String spaceComplexity;

	/** Description of the algorithm strategy */
	public String strategyDesc;

	/**
	 * Creates metrics with complexity labels.
	 *
	 *  name Algorithm name
	 *  tc   Time complexity
	 *  sc   Space complexity
	 *  desc Strategy description
	 */
	public AlgoMetrics(String name, String tc, String sc, String desc) {
	    this.algoName = name;
	    this.timeComplexity = tc;
	    this.spaceComplexity = sc;
	    this.strategyDesc = desc;
	}

	/** Returns step time formatted as milliseconds. */
	public String stepTimeMs() {
	    return String.format("%.3f ms", stepTimeNs / 1_000_000.0);
	}

	/** Returns total time formatted as milliseconds. */
	public String totalTimeMs() {
	    return String.format("%.2f ms", totalTimeNs / 1_000_000.0);
	}
}
