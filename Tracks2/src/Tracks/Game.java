package Tracks;


import java.util.*;

public class Game {
    public int w, h;
    public Cell[][] board;
    public TType[][] sol;
    public int sx, sy, ex, ey;
    private Random rnd = new Random();
    public Deque<Move> hist = new ArrayDeque<>();
    public GNode[][] graph;
    public boolean revealedSolution = false;

    public Game(int w, int h) {
        this.w = w;
        this.h = h;
        newBoard();
    }

    private void newBoard() {
        board = new Cell[h][w];
        sol = new TType[h][w];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                board[y][x] = new Cell();
                sol[y][x] = TType.EMPTY;
            }
        }
        hist.clear();
        graph = new GNode[h][w];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                graph[y][x] = new GNode(x, y);
            }
        }
        revealedSolution = false;
    }

    public boolean genPathAndSolution(int diffPercent) {
        newBoard();
        sy = rnd.nextInt(h);
        sx = 0;
        ex = rnd.nextInt(w);
        ey = h - 1;

        boolean[][] vis = new boolean[h][w];
        List<int[]> path = new ArrayList<>();
        int maxTries = 20000;
        int[] tries = new int[]{0};

        boolean ok = walkRec(sx, sy, vis, path, tries, maxTries);
        if (!ok) {
            return false;
        }

        int[] last = path.get(path.size() - 1);
        int lx = last[0], ly = last[1];
        if (ly != h - 1) {
            int cx = lx, cy = ly;
            while (cy < h - 1 && !vis[cy + 1][cx]) {
                cy++;
                path.add(new int[]{cx, cy});
                vis[cy][cx] = true;
            }
            if (cy != h - 1) {
                return false;
            }
            ex = cx;
            ey = cy;
        } else {
            ex = last[0];
            ey = last[1];
        }

        for (int i = 0; i < path.size(); i++) {
            int x = path.get(i)[0], y = path.get(i)[1];
            Set<Dir> s = new HashSet<>();
            if (i == 0) {
                s.add(Dir.L);
            } else {
                int px = path.get(i - 1)[0], py = path.get(i - 1)[1];
                if (px == x - 1) s.add(Dir.L);
                if (px == x + 1) s.add(Dir.R);
                if (py == y - 1) s.add(Dir.U);
                if (py == y + 1) s.add(Dir.D);
            }
            if (i == path.size() - 1) {
                s.add(Dir.D);
            } else {
                int nx = path.get(i + 1)[0], ny = path.get(i + 1)[1];
                if (nx == x - 1) s.add(Dir.L);
                if (nx == x + 1) s.add(Dir.R);
                if (ny == y - 1) s.add(Dir.U);
                if (ny == y + 1) s.add(Dir.D);
            }
            sol[y][x] = Util.typeFromDirs(s);
        }

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                board[y][x].t = TType.EMPTY;
                board[y][x].clue = false;
                board[y][x].start = board[y][x].end = false;
            }
        }
        board[sy][sx].start = true;
        board[ey][ex].end = true;

        board[sy][sx].t = sol[sy][sx];
        board[sy][sx].clue = true;
        board[ey][ex].t = sol[ey][ex];
        board[ey][ex].clue = true;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (sol[y][x] != TType.EMPTY && !(x == sx && y == sy) && !(x == ex && y == ey)) {
                    if (rnd.nextInt(100) < diffPercent) {
                        board[y][x].t = sol[y][x];
                        board[y][x].clue = true;
                    }
                }
            }
        }

        rebuildGraph();
        return true;
    }

    private boolean walkRec(int x, int y, boolean[][] vis, List<int[]> path, int[] tries, int maxTries) {
        if (tries[0]++ > maxTries) {
            return false;
        }
        vis[y][x] = true;
        path.add(new int[]{x, y});

        List<int[]> dirs = new ArrayList<>();
        dirs.add(new int[]{1, 0});
        dirs.add(new int[]{-1, 0});
        dirs.add(new int[]{0, 1});
        dirs.add(new int[]{0, -1});
        Collections.shuffle(dirs, rnd);

        for (int[] d : dirs) {
            int nx = x + d[0], ny = y + d[1];
            if (nx < 0 || nx >= w || ny < 0 || ny >= h) {
                continue;
            }
            if (!vis[ny][nx]) {
                if (walkRec(nx, ny, vis, path, tries, maxTries)) {
                    return true;
                }
            }
        }

        if (y == h - 1) {
            return true;
        }

        path.remove(path.size() - 1);
        vis[y][x] = false;
        return false;
    }

    public void rebuildGraph() {
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                graph[y][x].nbrs.clear();
            }
        }

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                Set<Dir> ds = Util.dirsOf(board[y][x].t);
                if (ds.isEmpty()) {
                    continue;
                }
                for (Dir d : ds) {
                    int nx = x + (d == Dir.R ? 1 : (d == Dir.L ? -1 : 0));
                    int ny = y + (d == Dir.D ? 1 : (d == Dir.U ? -1 : 0));
                    if (nx < 0 || nx >= w || ny < 0 || ny >= h) {
                        continue;
                    }
                    Set<Dir> nd = Util.dirsOf(board[ny][nx].t);
                    if (nd.contains(Util.opposite(d))) {
                        graph[y][x].nbrs.add(graph[ny][nx]);
                    }
                }
            }
        }
    }

    public List<int[]> findPathFromCurrent() {
        rebuildGraph();
        boolean[][] vis = new boolean[h][w];
        Map<String, String> prev = new HashMap<>();
        Deque<GNode> q = new ArrayDeque<>();
        q.add(graph[sy][sx]);
        vis[sy][sx] = true;
        prev.put(sx + "," + sy, null);

        while (!q.isEmpty()) {
            GNode cur = q.poll();
            if (cur.x == ex && cur.y == ey) {
                List<int[]> res = new ArrayList<>();
                String k = cur.x + "," + cur.y;
                while (k != null) {
                    String[] pr = k.split(",");
                    res.add(0, new int[]{Integer.parseInt(pr[0]), Integer.parseInt(pr[1])});
                    k = prev.get(k);
                }
                return res;
            }
            for (GNode n : cur.nbrs) {
                if (!vis[n.y][n.x]) {
                    vis[n.y][n.x] = true;
                    prev.put(n.x + "," + n.y, cur.x + "," + cur.y);
                    q.add(n);
                }
            }
        }
        return null;
    }

    public boolean graphPathExists() {
        rebuildGraph();
        boolean[][] vis = new boolean[h][w];
        Deque<GNode> q = new ArrayDeque<>();
        q.add(graph[sy][sx]);
        vis[sy][sx] = true;

        while (!q.isEmpty()) {
            GNode cur = q.poll();
            if (cur.x == ex && cur.y == ey) {
                return true;
            }
            for (GNode n : cur.nbrs) {
                if (!vis[n.y][n.x]) {
                    vis[n.y][n.x] = true;
                    q.add(n);
                }
            }
        }
        return false;
    }

    public boolean computerMove() {
        int[][] dist = bfsDistFromEnd();

        int bestD = Integer.MAX_VALUE;
        int bx = -1, by = -1;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (sol[y][x] != TType.EMPTY && board[y][x].t != sol[y][x]) {
                    int d = dist[y][x];
                    if (d >= 0 && d < bestD) {
                        bestD = d;
                        bx = x;
                        by = y;
                    }
                }
            }
        }
        if (bx == -1) {
            return false;
        }

        hist.push(new Move(bx, by, board[by][bx].t, board[by][bx].clue));
        board[by][bx].t = Util.nextType(board[by][bx].t);

        rebuildGraph();
        return true;
    }

    private int[][] bfsDistFromEnd() {
        int[][] dist = new int[h][w];
        for (int[] row : dist) {
            Arrays.fill(row, -1);
        }
        Deque<int[]> q = new ArrayDeque<>();
        q.add(new int[]{ex, ey});
        dist[ey][ex] = 0;
        while (!q.isEmpty()) {
            int[] v = q.poll();
            int x = v[0], y = v[1];
            int[][] d4 = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
            for (int[] d : d4) {
                int nx = x + d[0], ny = y + d[1];
                if (nx < 0 || nx >= w || ny < 0 || ny >= h) {
                    continue;
                }
                if (dist[ny][nx] == -1) {
                    dist[ny][nx] = dist[y][x] + 1;
                    q.add(new int[]{nx, ny});
                }
            }
        }
        return dist;
    }

    public void undo() {
        if (revealedSolution) {
            return;
        }
        if (hist.isEmpty()) {
            return;
        }
        Move m = hist.pop();
        board[m.y][m.x].t = m.prev;
        board[m.y][m.x].clue = m.prevClue;
        rebuildGraph();
    }

    public void restart() {
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (board[y][x].clue) {
                    board[y][x].t = sol[y][x];
                } else {
                    board[y][x].t = TType.EMPTY;
                }
            }
        }
        hist.clear();
        revealedSolution = false;
        rebuildGraph();
    }

    public void revealSolution() {
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                board[y][x].t = sol[y][x];
            }
        }
        revealedSolution = true;
        rebuildGraph();
    }

    public boolean checkSolved() {
        return graphPathExists();
    }
}
