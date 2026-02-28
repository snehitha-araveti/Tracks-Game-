package Tracks;

import java.util.*;

/**
 * Core game model for the Tracks puzzle.
 * Handles board state, puzzle generation, graph building, and player actions.
 *
 * @author [Your Name]
 */
public class Game {

    public int w, h;                  // Board width and height
    public Cell[][] board;            // Current board state
    public TType[][] sol;             // Solution path
    public int sx, sy, ex, ey;       // Start and end coordinates
    public int[] rowClues, colClues;  // Solution counts per row/column
    private Random rnd = new Random();
    public Deque<Move> hist = new ArrayDeque<>();  // Move history for undo
    public GNode[][] graph;           // Connectivity graph
    public boolean revealedSolution = false;

    /** Creates a new game with given dimensions. */
    public Game(int w, int h) {
        this.w = w; this.h = h;
        newBoard();
    }

    /** Initializes empty board and solution arrays. */
    private void newBoard() {
        board = new Cell[h][w];
        sol   = new TType[h][w];
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++) {
                board[y][x] = new Cell();
                sol[y][x]   = TType.EMPTY;
            }
        hist.clear();
        graph = new GNode[h][w];
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
                graph[y][x] = new GNode(x, y);
        rowClues = new int[h];
        colClues = new int[w];
        revealedSolution = false;
    }

    // ═════════════════════════════════════════════════════════════════════
    //  PUZZLE GENERATION
    // ═════════════════════════════════════════════════════════════════════
    /**
     * Generates a random puzzle with given difficulty percentage.
     * Uses divide-and-conquer approach to create a winding path.
     *
     * @param diffPercent  Percentage of solution cells to reveal as clues
     * @return true if puzzle generated successfully
     */
    public boolean genPathAndSolution(int diffPercent) {
        newBoard();

        // Set start (left side) and initial end (bottom row)
        sy = rnd.nextInt(h); sx = 0;
        ex = rnd.nextInt(w); ey = h - 1;

        boolean[][] vis = new boolean[h][w];
        List<int[]> path = new ArrayList<>();
        int[] tries = new int[]{0};

        int mid = (h - 1) / 2;

        // Generate path in top half first
        if (sy <= mid) {
            boolean ok = walkRec(sx, sy, vis, path, tries, 20000, 0, w-1, 0, mid);
            if (!ok || path.get(path.size()-1)[1] != mid) return false;

            // Connect to bottom half
            int hx = path.get(path.size()-1)[0];
            boolean found = false;
            for (int dx : new int[]{0, 1, -1, 2, -2}) {
                int nx = hx + dx, ny = mid + 1;
                if (nx < 0 || nx >= w || vis[ny][nx]) continue;
                if (walkRec(nx, ny, vis, path, tries, 20000, 0, w-1, mid+1, h-1)) { found=true; break; }
            }
            if (!found) return false;
        } else {
            if (!walkRec(sx, sy, vis, path, tries, 20000, 0, w-1, 0, h-1)) return false;
        }

        // Extend to bottom row
        int[] last = path.get(path.size()-1);
        int lx = last[0], ly = last[1];
        while (ly < h-1 && !vis[ly+1][lx]) { ly++; path.add(new int[]{lx, ly}); vis[ly][lx]=true; }
        if (ly != h-1) return false;
        ex = lx; ey = ly;

        // Convert path to track types
        for (int i = 0; i < path.size(); i++) {
            int x = path.get(i)[0], y = path.get(i)[1];
            Set<Dir> s = new HashSet<>();
            if (i == 0) { s.add(Dir.L); }
            else { int px=path.get(i-1)[0], py=path.get(i-1)[1];
                if (px==x-1) s.add(Dir.L); if (px==x+1) s.add(Dir.R);
                if (py==y-1) s.add(Dir.U); if (py==y+1) s.add(Dir.D); }
            if (i == path.size()-1) { s.add(Dir.D); }
            else { int nx=path.get(i+1)[0], ny=path.get(i+1)[1];
                if (nx==x-1) s.add(Dir.L); if (nx==x+1) s.add(Dir.R);
                if (ny==y-1) s.add(Dir.U); if (ny==y+1) s.add(Dir.D); }
            sol[y][x] = Util.typeFromDirs(s);
        }

        // Initialize board cells with clues
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++) { board[y][x] = new Cell(); }
        board[sy][sx].t = sol[sy][sx]; board[sy][sx].clue = true; board[sy][sx].start = true;
        board[ey][ex].t = sol[ey][ex]; board[ey][ex].clue = true; board[ey][ex].end   = true;

        // Reveal random clues based on difficulty
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
                if (sol[y][x] != TType.EMPTY && !(x==sx&&y==sy) && !(x==ex&&y==ey))
                    if (rnd.nextInt(100) < diffPercent) { board[y][x].t = sol[y][x]; board[y][x].clue = true; }

        computeClues();
        rebuildGraph();
        return true;
    }

    /** Computes row and column clue counts from the solution. */
    private void computeClues() {
        rowClues = new int[h]; colClues = new int[w];
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
                if (sol[y][x] != TType.EMPTY) { rowClues[y]++; colClues[x]++; }
    }

    /**
     * Recursive path walking with bounds and randomness.
     * Used for puzzle generation.
     */
    private boolean walkRec(int x, int y, boolean[][] vis, List<int[]> path,
                            int[] tries, int max, int x0, int x1, int y0, int y1) {
        if (tries[0]++ > max) return false;
        vis[y][x] = true; path.add(new int[]{x, y});
        List<int[]> dirs = new ArrayList<>(Arrays.asList(new int[]{1,0}, new int[]{-1,0},
                                                          new int[]{0,1}, new int[]{0,-1}));
        Collections.shuffle(dirs, rnd);
        for (int[] d : dirs) {
            int nx=x+d[0], ny=y+d[1];
            if (nx<x0||nx>x1||ny<y0||ny>y1) continue;
            if (!vis[ny][nx] && walkRec(nx, ny, vis, path, tries, max, x0, x1, y0, y1)) return true;
        }
        if (y == y1) return true;
        path.remove(path.size()-1); vis[y][x]=false; return false;
    }

    // ═════════════════════════════════════════════════════════════════════
    //  GRAPH HELPERS
    // ═════════════════════════════════════════════════════════════════════
    /** Rebuilds the connectivity graph from current board state. */
    public void rebuildGraph() {
        for (int y=0;y<h;y++) for (int x=0;x<w;x++) graph[y][x].nbrs.clear();
        for (int y=0;y<h;y++) for (int x=0;x<w;x++) {
            Set<Dir> ds = Util.dirsOf(board[y][x].t);
            for (Dir d : ds) {
                int nx=x+(d==Dir.R?1:d==Dir.L?-1:0);
                int ny=y+(d==Dir.D?1:d==Dir.U?-1:0);
                if (nx<0||nx>=w||ny<0||ny>=h) continue;
                if (Util.dirsOf(board[ny][nx].t).contains(Util.opposite(d)))
                    graph[y][x].nbrs.add(graph[ny][nx]);
            }
        }
    }

    /** Finds path from start to end using BFS. Returns list of coordinates or null. */
    public List<int[]> findPathFromCurrent() {
        rebuildGraph();
        boolean[][] vis = new boolean[h][w];
        Map<String,String> prev = new HashMap<>();
        Deque<GNode> q = new ArrayDeque<>();
        q.add(graph[sy][sx]); vis[sy][sx]=true; prev.put(sx+","+sy, null);
        while (!q.isEmpty()) {
            GNode cur = q.poll();
            if (cur.x==ex && cur.y==ey) {
                List<int[]> res = new ArrayList<>();
                String k = cur.x+","+cur.y;
                while (k!=null) { String[] p=k.split(","); res.add(0, new int[]{Integer.parseInt(p[0]),Integer.parseInt(p[1])}); k=prev.get(k); }
                return res;
            }
            for (GNode n : cur.nbrs) if (!vis[n.y][n.x]) { vis[n.y][n.x]=true; prev.put(n.x+","+n.y, cur.x+","+cur.y); q.add(n); }
        }
        return null;
    }

    /** Checks if a valid path exists from start to end. */
    public boolean graphPathExists() {
        rebuildGraph();
        boolean[][] vis = new boolean[h][w];
        Deque<GNode> q = new ArrayDeque<>();
        q.add(graph[sy][sx]); vis[sy][sx]=true;
        while (!q.isEmpty()) {
            GNode cur = q.poll();
            if (cur.x==ex && cur.y==ey) return true;
            for (GNode n : cur.nbrs) if (!vis[n.y][n.x]) { vis[n.y][n.x]=true; q.add(n); }
        }
        return false;
    }

    /** Returns true if puzzle is solved. */
    public boolean checkSolved() { return graphPathExists(); }

    // ═════════════════════════════════════════════════════════════════════
    //  PLAYER ACTIONS
    // ═════════════════════════════════════════════════════════════════════
    /** Undoes the last move. */
    public void undo() {
        if (revealedSolution || hist.isEmpty()) return;
        Move m = hist.pop();
        board[m.y][m.x].t = m.prev; board[m.y][m.x].clue = m.prevClue;
        rebuildGraph();
    }

    /** Resets board to initial state (clues only). */
    public void restart() {
        for (int y=0;y<h;y++) for (int x=0;x<w;x++)
            board[y][x].t = board[y][x].clue ? sol[y][x] : TType.EMPTY;
        hist.clear(); revealedSolution=false; rebuildGraph();
    }

    /** Reveals the full solution. */
    public void revealSolution() {
        for (int y=0;y<h;y++) for (int x=0;x<w;x++) board[y][x].t = sol[y][x];
        revealedSolution=true; rebuildGraph();
    }

    /** Creates a copy of the current board (used by algorithms). */
    public Cell[][] copyBoard() {
        Cell[][] copy = new Cell[h][w];
        for (int y=0;y<h;y++) for (int x=0;x<w;x++) copy[y][x] = board[y][x].copy();
        return copy;
    }

    /** BFS distance from end point over all cells. */
    public int[][] bfsDistFromEnd() {
        int[][] dist = new int[h][w];
        for (int[] row : dist) Arrays.fill(row, -1);
        Deque<int[]> q = new ArrayDeque<>();
        q.add(new int[]{ex, ey}); dist[ey][ex]=0;
        while (!q.isEmpty()) {
            int[] v=q.poll(); int x=v[0], y=v[1];
            for (int[] d : new int[][]{{1,0},{-1,0},{0,1},{0,-1}}) {
                int nx=x+d[0], ny=y+d[1];
                if (nx>=0&&nx<w&&ny>=0&&ny<h&&dist[ny][nx]==-1) { dist[ny][nx]=dist[y][x]+1; q.add(new int[]{nx,ny}); }
            }
        }
        return dist;
    }
}
