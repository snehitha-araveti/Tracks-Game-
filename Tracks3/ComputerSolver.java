package Tracks;

import java.util.*;

/**
 * ComputerSolver — implements four algorithms for the Tracks puzzle.
 *
 * KEY FACTS: - sol[y][x] stores the correct TType for the solution path. - The
 * solution is a single winding path from (sx,sy) to (ex,ey). - rowClues[y] /
 * colClues[x] = number of solution cells in each row/col. - step() places
 * exactly ONE piece per call (for animation).
 *
 * ALGORITHM SUMMARY: - Greedy: O(N²) — BFS from end each step, pick closest
 * unsolved cell - Divide & Con: O(N log N) — chain-follow path, split in half,
 * fill midpoint - DP: O(N) — build dp table once, replay each step -
 * Backtracking: O(N·2^N) worst / O(N) typical — recursive explore + undo
 */
public class ComputerSolver {

    public enum Algo {
        GREEDY, DC, DP, BACKTRACKING
    }

    public final Game game;
    private final Algo algo;
    private final AlgoMetrics metrics;

    private long cumulativeTimeNs = 0;
    private int cumulativeOps = 0;
    private int totalMoves = 0;
    private final List<long[]> stepLog = new ArrayList<>();

    // Pre-computed play order for DC, DP, Backtracking
    private List<int[]> playList = null;
    private int playIndex = 0;
    private int initOps = 0;

    /**
     * Creates a solver with the specified algorithm.
     */
    public ComputerSolver(Game game, Algo algo) {
        this.game = game;
        this.algo = algo;
        switch (algo) {
            case GREEDY:
                metrics = new AlgoMetrics("Greedy", "O(N²)", "O(N)",
                        "Each step: BFS from end picks closest unsolved cell.");
                break;
            case DC:
                metrics = new AlgoMetrics("Divide & Conquer", "O(N log N)", "O(N)",
                        "Chain-follows path, splits in half, fills midpoint first.");
                break;
            case DP:
                metrics = new AlgoMetrics("Dynamic Programming", "O(N)", "O(N)",
                        "dp table built once via chain-follow, replay each step.");
                break;
            default:
                metrics = new AlgoMetrics("Backtracking", "O(N·2^N) worst / O(N) avg", "O(N)",
                        "Recursive DFS with undo on constraint violation.");
        }
    }

    /**
     * Places ONE piece per call. Returns false when done.
     */
    public boolean step() {
        long t0 = System.nanoTime();
        int[] ops = {0};
        int[] pos = null;
        int space = 0;

        switch (algo) {
            case GREEDY:
                pos = stepGreedy(ops);
                space = game.w * game.h;
                break;
            case DC:
                if (playList == null) {
                    buildDCPlayList(ops);
                }
                pos = stepFromPlayList(ops);
                space = game.w * game.h;
                break;
            case DP:
                if (playList == null) {
                    buildDPPlayList(ops);
                }
                pos = stepFromPlayList(ops);
                space = game.w * game.h;
                break;
            case BACKTRACKING:
                if (playList == null) {
                    buildBTPlayList(ops);
                }
                pos = stepFromPlayList(ops);
                space = game.w * game.h;
                break;
        }

        long elapsed = System.nanoTime() - t0;
        cumulativeTimeNs += elapsed;
        cumulativeOps += ops[0];

        if (pos == null) {
            return false;
        }

        int bx = pos[0], by = pos[1];
        game.hist.push(new Move(bx, by, game.board[by][bx].t, game.board[by][bx].clue));
        game.board[by][bx].t = game.sol[by][bx];
        game.board[by][bx].clue = false;
        game.rebuildGraph();
        totalMoves++;

        stepLog.add(new long[]{elapsed, ops[0], space});
        metrics.stepTimeNs = elapsed;
        metrics.opsCount = ops[0];
        metrics.spaceUsed = space;
        metrics.totalMoves = totalMoves;
        metrics.totalTimeNs = cumulativeTimeNs;
        return true;
    }

    /**
     * Replays pre-computed play order. InitOps charged on first call only.
     */
    private int[] stepFromPlayList(int[] ops) {
        if (playList == null) {
            return null;
        }
        ops[0] += initOps;
        initOps = 0;
        while (playIndex < playList.size()) {
            int[] c = playList.get(playIndex++);
            ops[0]++;
            if (game.sol[c[1]][c[0]] != TType.EMPTY && game.board[c[1]][c[0]].t != game.sol[c[1]][c[0]]) {
                return new int[]{c[0], c[1]};
            }
        }
        return null;
    }

    // ═════════════════════════════════════════════════════════════════════
    //  CHAIN-FOLLOW PATH (core utility)
    // ═════════════════════════════════════════════════════════════════════
    /**
     * Walks the solution path in exact sequential order. Reads each cell's
     * TType to determine which neighbour to follow next. Avoids going backward
     * to the previous cell.
     *
     * @param ops Operation counter
     * @return Ordered list [start, ..., end]
     */
    private List<int[]> chainFollowPath(int[] ops) {
        List<int[]> path = new ArrayList<>();
        boolean[][] onPath = new boolean[game.h][game.w];

        // Mark solution cells
        for (int y = 0; y < game.h; y++) {
            for (int x = 0; x < game.w; x++) {
                if (game.sol[y][x] != TType.EMPTY) {
                    onPath[y][x] = true;
                }
            }
        }

        int cx = game.sx, cy = game.sy;
        int px = -1, py = -1;   // previous cell

        while (true) {
            ops[0]++;
            path.add(new int[]{cx, cy});
            if (cx == game.ex && cy == game.ey) {
                break;
            }

            // Find forward neighbour (not previous, and on path)
            Set<Dir> dirs = Util.dirsOf(game.sol[cy][cx]);
            int nx = -1, ny = -1;
            for (Dir d : dirs) {
                int tx = cx + (d == Dir.R ? 1 : d == Dir.L ? -1 : 0);
                int ty = cy + (d == Dir.D ? 1 : d == Dir.U ? -1 : 0);
                if (tx < 0 || tx >= game.w || ty < 0 || ty >= game.h) {
                    continue;
                }
                if (!onPath[ty][tx]) {
                    continue;
                }
                if (tx == px && ty == py) {
                    continue;
                }
                nx = tx;
                ny = ty;
                break;
            }
            if (nx == -1) {
                break;
            }
            px = cx;
            py = cy;
            cx = nx;
            cy = ny;
        }
        return path;
    }

    // ═════════════════════════════════════════════════════════════════════
    //  2. DIVIDE & CONQUER ALGORITHM
    // ═════════════════════════════════════════════════════════════════════
    /**
     * Builds play order by recursively filling midpoint first.
     */
    private void buildDCPlayList(int[] ops) {
        List<int[]> path = chainFollowPath(ops);
        playList = new ArrayList<>(path.size());
        playIndex = 0;
        dcFill(path, 0, path.size() - 1, ops);
        initOps = ops[0];
    }

    /**
     * Recursive D&C: fill midpoint, then left and right halves.
     */
    private void dcFill(List<int[]> path, int lo, int hi, int[] ops) {
        ops[0]++;
        if (lo > hi) {
            return;
        }
        int mid = (lo + hi) / 2;
        playList.add(path.get(mid));            // midpoint first
        dcFill(path, lo, mid - 1, ops);   // left half
        dcFill(path, mid + 1, hi, ops);   // right half
    }

    // ═════════════════════════════════════════════════════════════════════
    //  4. BACKTRACKING ALGORITHM
    // ═════════════════════════════════════════════════════════════════════
    /** Builds play order via recursive DFS with constraint checking. */
    private void buildBTPlayList(int[] ops) {
        playList  = new ArrayList<>();
        playIndex = 0;

        // Collect all solution cells as candidates
        List<int[]> candidates = new ArrayList<>();
        for (int y = 0; y < game.h; y++)
            for (int x = 0; x < game.w; x++)
                if (game.sol[y][x] != TType.EMPTY)
                    candidates.add(new int[]{x, y});

        int[] rowTarget = game.rowClues.clone();
        int[] colTarget = game.colClues.clone();
        int[] rowCount  = new int[game.h];
        int[] colCount  = new int[game.w];
        boolean[] placed = new boolean[candidates.size()];

        boolean found = btRecurse(candidates, placed,
                                  rowTarget, colTarget, rowCount, colCount,
                                  0, ops);

        if (!found) playList = chainFollowPath(ops); // fallback
        initOps = ops[0];
    }

    /** Recursive backtracking with undo on constraint violation. */
    private boolean btRecurse(List<int[]> candidates, boolean[] placed,
                               int[] rowTarget, int[] colTarget,
                               int[] rowCount,  int[] colCount,
                               int placedCount, int[] ops) {
        ops[0]++;

        // Base case: all cells placed
        if (placedCount == candidates.size()) {
            for (int r = 0; r < rowTarget.length; r++)
                if (rowCount[r] != rowTarget[r]) return false;
            for (int c = 0; c < colTarget.length; c++)
                if (colCount[c] != colTarget[c]) return false;
            return true;
        }

        for (int i = 0; i < candidates.size(); i++) {
            if (placed[i]) continue;
            ops[0]++;

            int x = candidates.get(i)[0];
            int y = candidates.get(i)[1];

            // Pruning: check row/col clue constraints
            if (rowCount[y] + 1 > rowTarget[y]) continue;
            if (colCount[x] + 1 > colTarget[x]) continue;

            // Try this cell
            placed[i] = true;
            rowCount[y]++;
            colCount[x]++;
            playList.add(new int[]{x, y});

            if (btRecurse(candidates, placed, rowTarget, colTarget,
                          rowCount, colCount, placedCount + 1, ops))
                return true;

            // Undo (backtrack)
            playList.remove(playList.size() - 1);
            rowCount[y]--;
            colCount[x]--;
            placed[i] = false;
        }
        return false;
    }

    public AlgoMetrics getMetrics() {
        return metrics;
    }

    public int getTotalMoves() {
        return totalMoves;
    }

    public long getCumulativeTimeNs() {
        return cumulativeTimeNs;
    }

    public int getCumulativeOps() {
        return cumulativeOps;
    }

    public List<long[]> getStepLog() {
        return stepLog;
    }
}
