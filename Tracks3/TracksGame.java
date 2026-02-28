package Tracks;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * TracksGame 
 * 
 * Features:
 * - Two boards side-by-side: player (left) and computer (right)
 * - Algorithm selector in new game dialog
 * - Step-by-step computer solver with animation
 * - Analysis dialog showing metrics and comparison
 */
public class TracksGame extends JFrame {

    // ═════════════════════════════════════════════════════════════════════
    //  STATE
    // ═════════════════════════════════════════════════════════════════════
    private Game userGame;                     // Player's board
    private Game compGame;                     // Computer's board (separate clone)
    private ComputerSolver solver;             // Algorithm solver
    private ComputerSolver.Algo selectedAlgo = ComputerSolver.Algo.GREEDY;

    private int setW = 8, setH = 8, setDiff = 35;  // Game settings

    private boolean userSolved = false;
    private boolean compSolved = false;
    private int     userMoves  = 0;

    private long userStartMs, userEndMs;
    private long compStartMs, compEndMs;

    private Timer compTimer;  // Timer for animated computer solving

    // ═════════════════════════════════════════════════════════════════════
    //  UI COMPONENTS
    // ═════════════════════════════════════════════════════════════════════
    private BoardPanel userBoard, compBoard;
    private JLabel     lblMsg;
    private JButton    btnNew, btnRestart, btnUndo, btnSolve, btnCheck;
    private JButton    btnRunComp, btnAnalysis;
    private JPanel     topBar;

    /** Creates the main application window. */
    public TracksGame() {
        super("Tracks — Review 3");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1100, 720);
        setMinimumSize(new Dimension(860, 560));
        setLocationRelativeTo(null);
        initUI();
        setVisible(true);
        SwingUtilities.invokeLater(this::newGameDialog);
    }

    // ═════════════════════════════════════════════════════════════════════
    //  UI INITIALIZATION
    // ═════════════════════════════════════════════════════════════════════
    /** Sets up the user interface. */
    private void initUI() {
        setLayout(new BorderLayout());

        // Top bar with buttons (Review 2 style)
        topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));

        btnNew      = colorBtn("New Game",       new Color(76,  175, 80));
        btnRestart  = colorBtn("Restart",         new Color(255, 193, 7));
        btnUndo     = colorBtn("Undo",            new Color(33,  150, 243));
        btnSolve    = colorBtn("Show Solution",   new Color(244, 67,  54));
        btnCheck    = colorBtn("Check",           new Color(156, 39,  176));
        btnRunComp  = colorBtn("▶ Run Computer",  new Color(0,   150, 136));
        btnAnalysis = colorBtn("📊 Analysis",     new Color(100, 100, 100));
        btnAnalysis.setEnabled(false);

        topBar.add(btnNew);
        topBar.add(btnRestart);
        topBar.add(btnUndo);
        topBar.add(btnSolve);
        topBar.add(btnCheck);
        topBar.add(new JSeparator(JSeparator.VERTICAL));
        topBar.add(btnRunComp);
        topBar.add(btnAnalysis);

        lblMsg = new JLabel("Welcome to Tracks — Review 3");
        topBar.add(lblMsg);
        add(topBar, BorderLayout.NORTH);

        // Two board panels side by side
        userBoard = new BoardPanel(true);
        compBoard = new BoardPanel(false);   // Computer board is read-only

        userBoard.setMoveListener(this::handleUserMove);

        JPanel center = new JPanel(new GridLayout(1, 2, 4, 0));
        center.add(wrapBoard(userBoard, "👤 Your Board"));
        center.add(wrapBoard(compBoard, "🤖 Computer Board"));
        add(center, BorderLayout.CENTER);

        // Button listeners
        btnNew    .addActionListener(e -> newGameDialog());
        btnRestart.addActionListener(e -> handleRestart());
        btnUndo   .addActionListener(e -> handleUndo());
        btnSolve  .addActionListener(e -> handleReveal());
        btnCheck  .addActionListener(e -> handleCheck());
        btnRunComp.addActionListener(e -> startComputerSolve());
        btnAnalysis.addActionListener(e -> showAnalysisDialog());
    }

    /** Wraps a BoardPanel with a titled border. */
    private JPanel wrapBoard(BoardPanel bp, String title) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createTitledBorder(title));
        p.add(new JScrollPane(bp), BorderLayout.CENTER);
        return p;
    }

    // ═════════════════════════════════════════════════════════════════════
    //  NEW GAME
    // ═════════════════════════════════════════════════════════════════════
    /** Shows dialog to configure new game settings. */
    private void newGameDialog() {
        if (compTimer != null && compTimer.isRunning()) compTimer.stop();

        JPanel p = new JPanel(new GridLayout(4, 2, 6, 6));

        JTextField tfW = new JTextField("" + setW);
        JTextField tfH = new JTextField("" + setH);

        String[] diffs = {"Easy (50%)", "Medium (35%)", "Hard (20%)"};
        JComboBox<String> cbDiff = new JComboBox<>(diffs);
        cbDiff.setSelectedIndex(setDiff >= 45 ? 0 : setDiff >= 30 ? 1 : 2);

        String[] algos = {"Greedy", "Divide & Conquer", "Dynamic Programming", "Backtracking"};
        JComboBox<String> cbAlgo = new JComboBox<>(algos);
        cbAlgo.setSelectedIndex(selectedAlgo.ordinal());

        p.add(new JLabel("Width:"));   p.add(tfW);
        p.add(new JLabel("Height:"));  p.add(tfH);
        p.add(new JLabel("Difficulty:")); p.add(cbDiff);
        p.add(new JLabel("Computer Algorithm:")); p.add(cbAlgo);

        int res = JOptionPane.showConfirmDialog(this, p, "New Game Settings",
                JOptionPane.OK_CANCEL_OPTION);
        if (res != JOptionPane.OK_OPTION) return;

        try {
            int nw = Integer.parseInt(tfW.getText().trim());
            int nh = Integer.parseInt(tfH.getText().trim());
            if (nw < 4 || nh < 4 || nw > 14 || nh > 14) throw new NumberFormatException();
            setW = nw; setH = nh;
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid size; using defaults.");
        }
        int di = cbDiff.getSelectedIndex();
        setDiff = di == 0 ? 50 : di == 1 ? 35 : 20;
        selectedAlgo = ComputerSolver.Algo.values()[cbAlgo.getSelectedIndex()];

        startNewGame();
    }

    /** Starts a new game with current settings. */
    private void startNewGame() {
        // Generate puzzle
        Game g = null;
        for (int t = 0; t < 50 && g == null; t++) {
            Game tmp = new Game(setW, setH);
            if (tmp.genPathAndSolution(setDiff)) g = tmp;
        }
        if (g == null) {
            JOptionPane.showMessageDialog(this, "Couldn't generate puzzle — try different size.");
            return;
        }

        userGame = g;
        compGame = cloneGame(g);
        solver   = new ComputerSolver(compGame, selectedAlgo);

        userSolved = false; compSolved = false;
        userMoves  = 0;
        userStartMs = System.currentTimeMillis();
        compStartMs = System.currentTimeMillis();
        userEndMs   = 0; compEndMs = 0;

        userBoard.setGame(userGame);
        compBoard.setGame(compGame);

        btnRunComp.setEnabled(true);
        btnAnalysis.setEnabled(false);

        setMessage("New " + setW + "×" + setH + " game  |  Algorithm: " +
                   solver.getMetrics().algoName + "  |  Left-click = next track, Right-click = clear");
    }

    // ═════════════════════════════════════════════════════════════════════
    //  PLAYER ACTIONS
    // ═════════════════════════════════════════════════════════════════════
    /** Handles player's move on the board. */
    private void handleUserMove(int x, int y) {
        if (userGame == null || userSolved || userGame.revealedSolution) {
            setMessage("Restart to play again."); return;
        }
        if (userGame.board[y][x].clue) { setMessage("Cell is a clue (locked)"); return; }

        userGame.hist.push(new Move(x, y, userGame.board[y][x].t, false));
        userGame.board[y][x].t = Util.nextType(userGame.board[y][x].t);
        userGame.rebuildGraph();
        userMoves++;
        userBoard.highlightPath = false;
        userBoard.repaint();

        if (userGame.checkSolved()) {
            userEndMs  = System.currentTimeMillis();
            userSolved = true;
            userBoard.highlightPath = true;
            userBoard.repaint();
            setMessage("🎉 Solved!  Moves: " + userMoves + "  Time: " +
                       String.format("%.1f s", (userEndMs - userStartMs) / 1000.0));
            btnAnalysis.setEnabled(true);
        } else {
            setMessage("Moves: " + userMoves + "  |  Keep going!");
        }
    }

    /** Undoes the last player move. */
    private void handleUndo() {
        if (userGame == null || userSolved) return;
        userGame.undo();
        userMoves = Math.max(0, userMoves - 1);
        userBoard.highlightPath = false;
        userBoard.repaint();
        setMessage("Undo  |  Moves: " + userMoves);
    }

    /** Restarts the current game. */
    private void handleRestart() {
        if (userGame == null) return;
        userGame.restart();
        userMoves  = 0;
        userSolved = false;
        userStartMs = System.currentTimeMillis();
        userBoard.highlightPath = false;
        userBoard.repaint();
        setMessage("Restarted.");
    }

    /** Reveals the solution on the player's board. */
    private void handleReveal() {
        if (userGame == null) return;
        userGame.revealSolution();
        userBoard.highlightPath = true;
        userBoard.repaint();
        setMessage("Solution shown — Restart to play again.");
    }

    /** Checks if the player's board is solved. */
    private void handleCheck() {
        if (userGame == null) return;
        if (userGame.checkSolved()) {
            userBoard.highlightPath = true;
            userBoard.repaint();
            setMessage("✅ Solved!");
        } else {
            setMessage("❌ Not solved yet — keep trying.");
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    //  COMPUTER SOLVER
    // ═════════════════════════════════════════════════════════════════════
    /** Starts the computer solver with animated steps. */
    private void startComputerSolve() {
        if (compTimer != null && compTimer.isRunning()) return;
        if (compSolved || solver == null) return;
        btnRunComp.setEnabled(false);
        compStartMs = System.currentTimeMillis();
        setMessage("🤖 " + solver.getMetrics().algoName + " is solving…");

        compTimer = new Timer(180, null);
        compTimer.addActionListener(e -> {
            boolean moved = solver.step();
            compBoard.repaint();

            if (solver.game.checkSolved()) {
                compEndMs  = System.currentTimeMillis();
                compSolved = true;
                compBoard.highlightPath = true;
                compBoard.repaint();
                compTimer.stop();
                setMessage("🤖 Computer solved in " + solver.getTotalMoves() + " steps  |  " +
                           String.format("%.1f s", (compEndMs - compStartMs) / 1000.0));
                btnAnalysis.setEnabled(true);
                return;
            }
            if (!moved) {
                compTimer.stop();
                setMessage("🤖 Computer finished — check path with Check button.");
                btnAnalysis.setEnabled(true);
            }
        });
        compTimer.start();
    }

    // ═════════════════════════════════════════════════════════════════════
    //  ANALYSIS DIALOG
    // ═════════════════════════════════════════════════════════════════════
    /** Shows the analysis dialog with performance metrics. */
    private void showAnalysisDialog() {
        long uMs = userEndMs > 0 ? (userEndMs - userStartMs) : (System.currentTimeMillis() - userStartMs);
        long cMs = compEndMs > 0 ? (compEndMs - compStartMs) : (System.currentTimeMillis() - compStartMs);

        AlgoMetrics am = solver.getMetrics();
        int N = setW * setH;

        StringBuilder sb = new StringBuilder();
        sb.append("═══════════════════════════════════════════\n");
        sb.append("          GAME ANALYSIS — REVIEW 3\n");
        sb.append("═══════════════════════════════════════════\n\n");

        sb.append("── PLAYER ──────────────────────────────\n");
        sb.append(String.format("  Status  : %s\n", userSolved ? "✅ Solved" : "❌ Not solved"));
        sb.append(String.format("  Moves   : %d\n", userMoves));
        sb.append(String.format("  Time    : %.2f s\n\n", uMs / 1000.0));

        sb.append("── COMPUTER (" + am.algoName + ") ─────────────────\n");
        sb.append(String.format("  Status  : %s\n", compSolved ? "✅ Solved" : "❌ Not solved"));
        sb.append(String.format("  Steps   : %d\n", solver.getTotalMoves()));
        sb.append(String.format("  Time    : %.2f s\n\n", cMs / 1000.0));

        sb.append("── ALGORITHM COMPLEXITY ─────────────────\n");
        sb.append(String.format("  Strategy : %s\n", am.strategyDesc));
        sb.append(String.format("  Time     : %s  (theoretical)\n", am.timeComplexity));
        sb.append(String.format("  Space    : %s  (theoretical)\n\n", am.spaceComplexity));

        sb.append("── MEASURED METRICS ─────────────────────\n");
        sb.append(String.format("  Board N        : %d cells  (%dx%d)\n", N, setW, setH));
        sb.append(String.format("  Total ops      : %,d\n", solver.getCumulativeOps()));
        sb.append(String.format("  Total algo time: %s\n", am.totalTimeMs()));
        int steps = solver.getTotalMoves();
        sb.append(String.format("  Avg / step     : %.3f ms\n\n",
                steps > 0 ? am.totalTimeNs / 1_000_000.0 / steps : 0));

        sb.append("── PER-STEP LOG (last 10) ───────────────\n");
        sb.append(String.format("  %-6s %-12s %-10s %-8s\n","Step","Time(ms)","Ops","Space"));
        java.util.List<long[]> log = solver.getStepLog();
        int from = Math.max(0, log.size() - 10);
        for (int i = from; i < log.size(); i++) {
            long[] s = log.get(i);
            sb.append(String.format("  #%-5d %-12s %-10d %-8d\n",
                    i + 1,
                    String.format("%.3f", s[0] / 1_000_000.0),
                    s[1], s[2]));
        }
        sb.append("\n");

        // Winner banner
        sb.append("═══════════════════════════════════════════\n");
        if (userSolved && compSolved) {
            if (uMs < cMs)       sb.append("  🏆 PLAYER WINS — faster by " + (cMs-uMs)/1000.0 + " s\n");
            else if (cMs < uMs)  sb.append("  🤖 COMPUTER WINS — faster by " + (uMs-cMs)/1000.0 + " s\n");
            else                 sb.append("  🤝 TIE!\n");
        } else if (userSolved)  sb.append("  🏆 PLAYER WINS — computer did not finish.\n");
        else if (compSolved)    sb.append("  🤖 COMPUTER WINS — player did not finish.\n");
        else                    sb.append("  ⏱  Neither side solved the puzzle.\n");
        sb.append("═══════════════════════════════════════════\n");

        JTextArea ta = new JTextArea(sb.toString(), 30, 52);
        ta.setFont(new Font("Monospaced", Font.PLAIN, 12));
        ta.setEditable(false);
        ta.setBackground(new Color(245, 245, 245));

        JOptionPane.showMessageDialog(this,
                new JScrollPane(ta),
                "Game Analysis",
                JOptionPane.INFORMATION_MESSAGE);
    }

    // ═════════════════════════════════════════════════════════════════════
    //  HELPERS
    // ═════════════════════════════════════════════════════════════════════
    /** Updates the status message. */
    public void setMessage(String msg) { lblMsg.setText("  " + msg); }

    /** Clones a game for independent play. */
    private Game cloneGame(Game src) {
        Game dst = new Game(src.w, src.h);
        dst.sx = src.sx; dst.sy = src.sy;
        dst.ex = src.ex; dst.ey = src.ey;
        dst.rowClues = src.rowClues.clone();
        dst.colClues = src.colClues.clone();
        for (int y = 0; y < src.h; y++)
            for (int x = 0; x < src.w; x++) {
                dst.sol[y][x]   = src.sol[y][x];
                dst.board[y][x] = src.board[y][x].copy();
            }
        dst.rebuildGraph();
        return dst;
    }

    /** Creates a colored button (Review 2 style). */
    private JButton colorBtn(String text, Color bg) {
        JButton b = new JButton(text);
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setOpaque(true);
        b.setBorderPainted(false);
        return b;
    }

    // ═════════════════════════════════════════════════════════════════════
    //  ENTRY POINT
    // ═════════════════════════════════════════════════════════════════════
    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception ignored) {}
        SwingUtilities.invokeLater(TracksGame::new);
    }
}
