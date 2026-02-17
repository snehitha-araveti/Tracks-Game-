package Tracks;

import java.util.*;


public class DynamicProgramming {
    
    private int width, height;
    private Map<String, Integer> memo; // Memoization table
    private DPMetrics metrics;
    
    public DynamicProgramming(int width, int height) {
        this.width = width;
        this.height = height;
        this.memo = new HashMap<>();
        this.metrics = new DPMetrics();
    }
    
    public int findMinimumMoves(TType[][] current, TType[][] solution) {
        long startTime = System.nanoTime();
        metrics.reset();
        
        String stateKey = encodeState(current);
        
        // Check if already computed
        if (memo.containsKey(stateKey)) {
            metrics.cacheHits++;
            return memo.get(stateKey);
        }
        
        // Base case: puzzle solved
        if (isSolved(current, solution)) {
            metrics.statesExplored++;
            return 0;
        }
        
        // Count differences
        int differences = countDifferences(current, solution);
        
        // Pruning: if too many differences, use heuristic
        if (differences > 20) {
            metrics.cacheMisses++;
            return differences; // Heuristic: at least this many moves
        }
        
        int minMoves = Integer.MAX_VALUE;
        
        // Try all possible next moves
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (current[y][x] != solution[y][x]) {
                    metrics.statesExplored++;
                    
                    // Make move
                    TType[][] nextState = copyBoard(current);
                    nextState[y][x] = solution[y][x];
                    
                    // Recursive call with memoization
                    int movesFromHere = 1 + findMinimumMoves(nextState, solution);
                    minMoves = Math.min(minMoves, movesFromHere);
                }
            }
        }
        
        // Memoize result
        memo.put(stateKey, minMoves);
        
        long endTime = System.nanoTime();
        metrics.executionTimeMs = endTime - startTime;
        
        return minMoves;
    }
    
    
    public List<int[]> findOptimalSequence(TType[][] current, TType[][] solution) {
        List<int[]> sequence = new ArrayList<>();
        TType[][] state = copyBoard(current);
        
        while (!isSolved(state, solution)) {
            int bestX = -1, bestY = -1;
            int bestValue = Integer.MAX_VALUE;
            
            // For each unfilled cell, calculate value of filling it
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    if (state[y][x] != solution[y][x]) {
                        TType[][] nextState = copyBoard(state);
                        nextState[y][x] = solution[y][x];
                        
                        int value = findMinimumMoves(nextState, solution);
                        
                        if (value < bestValue) {
                            bestValue = value;
                            bestX = x;
                            bestY = y;
                        }
                    }
                }
            }
            
            if (bestX == -1) break;
            
            // Make best move
            state[bestY][bestX] = solution[bestY][bestX];
            sequence.add(new int[]{bestX, bestY});
        }
        
        return sequence;
    }
    
    
    public boolean isSolvableInKMoves(TType[][] current, TType[][] solution, int k) {
        // DP table: dp[i][j] = can we fix first i cells in exactly j moves?
        int totalCells = width * height;
        boolean[][] dp = new boolean[totalCells + 1][k + 1];
        
        dp[0][0] = true; // Base case: 0 cells in 0 moves
        
        int cellIndex = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                cellIndex++;
                int cost = (current[y][x] != solution[y][x]) ? 1 : 0;
                
                for (int moves = 0; moves <= k; moves++) {
                    // Don't fix this cell
                    dp[cellIndex][moves] = dp[cellIndex - 1][moves];
                    
                    // Fix this cell (if needed and possible)
                    if (cost > 0 && moves >= cost) {
                        dp[cellIndex][moves] |= dp[cellIndex - 1][moves - cost];
                    }
                }
            }
        }
        
        
        for (int moves = 0; moves <= k; moves++) {
            if (dp[totalCells][moves]) return true;
        }
        
        return false;
    }
    
    
    public int calculateBoardSimilarity(TType[][] current, TType[][] solution) {
        // Flatten boards to 1D arrays
        List<TType> currentFlat = new ArrayList<>();
        List<TType> solutionFlat = new ArrayList<>();
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                currentFlat.add(current[y][x]);
                solutionFlat.add(solution[y][x]);
            }
        }
        
        int n = currentFlat.size();
        int[][] lcs = new int[n + 1][n + 1];
        
        // DP for LCS
        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= n; j++) {
                if (currentFlat.get(i - 1) == solutionFlat.get(j - 1)) {
                    lcs[i][j] = lcs[i - 1][j - 1] + 1;
                } else {
                    lcs[i][j] = Math.max(lcs[i - 1][j], lcs[i][j - 1]);
                }
            }
        }
        
        return lcs[n][n];
    }
    
    public int calculateEditDistance(TType[][] current, TType[][] solution) {
        int differences = 0;
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (current[y][x] != solution[y][x]) {
                    differences++;
                }
            }
        }
        
        return differences; // For this game, edit distance = simple difference count
    }
    
    // Helper methods
    
    private String encodeState(TType[][] board) {
        StringBuilder sb = new StringBuilder();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                sb.append(board[y][x].ordinal()).append(',');
            }
        }
        return sb.toString();
    }
    
    private boolean isSolved(TType[][] current, TType[][] solution) {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (current[y][x] != solution[y][x]) {
                    return false;
                }
            }
        }
        return true;
    }
    
    private int countDifferences(TType[][] current, TType[][] solution) {
        int count = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (current[y][x] != solution[y][x]) {
                    count++;
                }
            }
        }
        return count;
    }
    
    private TType[][] copyBoard(TType[][] board) {
        TType[][] copy = new TType[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                copy[y][x] = board[y][x];
            }
        }
        return copy;
    }
    
    public DPMetrics getMetrics() {
        return metrics;
    }
    
    public void clearMemo() {
        memo.clear();
    }
    
    public int getMemoSize() {
        return memo.size();
    }
}
