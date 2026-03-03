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
    private Game originalGame;                 // Pristine puzzle (never modified)
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
    private JButton    btnRunComp, btnRestartComp, btnChangeAlgo, btnAnalysis;
    private JPanel     topBar;

    private boolean firstLaunch = true;

    // Persistent analysis panel — accumulates all runs until New Game
    private final AnalysisPanel analysisPanel = new AnalysisPanel();

    /** Creates the main application window. */
    public TracksGame() {
        super("Tracks — Review 3");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1100, 720);
        setMinimumSize(new Dimension(860, 560));
        setLocationRelativeTo(null);
        initUI();
        newGameDialog();
        setVisible(true);
    }

    // ═════════════════════════════════════════════════════════════════════
    //  UI INITIALIZATION
    // ═════════════════════════════════════════════════════════════════════
    /** Sets up the user interface. */
    private void initUI() {
        setLayout(new BorderLayout());

        // Top bar with buttons (Review 2 style)
        topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));

        btnNew         = colorBtn("New Game",          new Color(76,  175, 80));
        btnRestart     = colorBtn("Restart",            new Color(255, 193, 7));
        btnUndo        = colorBtn("Undo",               new Color(33,  150, 243));
        btnSolve       = colorBtn("Show Solution",      new Color(244, 67,  54));
        btnCheck       = colorBtn("Check",              new Color(156, 39,  176));
        btnRunComp     = colorBtn("▶ Run Computer",     new Color(0,   150, 136));
        btnRestartComp = colorBtn("↺ Restart Computer", new Color(230, 120, 0));
        btnChangeAlgo  = colorBtn("⚙ Change Algorithm", new Color(90,  90,  160));
        btnAnalysis    = colorBtn("📊 Analysis",        new Color(100, 100, 100));
        btnAnalysis.setEnabled(false);
        btnRestartComp.setEnabled(false);
        btnChangeAlgo.setEnabled(false);

        topBar.add(btnNew);
        topBar.add(btnRestart);
        topBar.add(btnUndo);
        topBar.add(btnSolve);
        topBar.add(btnCheck);
        topBar.add(new JSeparator(JSeparator.VERTICAL));
        topBar.add(btnRunComp);
        topBar.add(btnRestartComp);
        topBar.add(btnChangeAlgo);
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
        btnRunComp    .addActionListener(e -> startComputerSolve());
        btnRestartComp.addActionListener(e -> handleRestartComputer());
        btnChangeAlgo .addActionListener(e -> handleChangeAlgo());
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

        JDialog dialog = new JDialog(this, "New Game Settings", true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setLayout(new BorderLayout(10, 10));

        JPanel iconPanel = new JPanel(new BorderLayout());
        iconPanel.add(new JLabel(UIManager.getIcon("OptionPane.questionIcon")), BorderLayout.NORTH);
        iconPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 0));

        JPanel content = new JPanel(new BorderLayout(10, 10));
        content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        content.add(iconPanel, BorderLayout.WEST);
        content.add(p, BorderLayout.CENTER);

        JButton btnOK = new JButton("OK");
        JButton btnCancel = new JButton("Cancel");
        final boolean[] confirmed = {false};
        btnOK.addActionListener(e -> { confirmed[0] = true; dialog.dispose(); });
        btnCancel.addActionListener(e -> dialog.dispose());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttons.add(btnOK);
        buttons.add(btnCancel);

        dialog.add(content, BorderLayout.CENTER);
        dialog.add(buttons, BorderLayout.SOUTH);
        dialog.getRootPane().setDefaultButton(btnOK);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);

        if (!confirmed[0]) {
            if (firstLaunch) { firstLaunch = false; startNewGame(); }
            return;
        }
        firstLaunch = false;

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

        userGame     = g;
        originalGame = cloneGame(g);   // pristine copy — never touched
        compGame     = cloneGame(g);
        solver   = new ComputerSolver(compGame, selectedAlgo);

        userSolved = false; compSolved = false;
        userMoves  = 0;
        analysisPanel.newGame();   // reset graph history for fresh game
        userStartMs = System.currentTimeMillis();
        compStartMs = System.currentTimeMillis();
        userEndMs   = 0; compEndMs = 0;

        userBoard.setGame(userGame);
        compBoard.setGame(compGame);

        btnRunComp.setEnabled(true);
        btnRestartComp.setEnabled(false);
        btnChangeAlgo.setEnabled(true);
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
    /** Resets the computer board and solver so it can be run again. */
    private void handleRestartComputer() {
        if (compTimer != null && compTimer.isRunning()) compTimer.stop();
        if (originalGame == null) return;
        compGame   = cloneGame(originalGame);   // always from pristine puzzle
        solver     = new ComputerSolver(compGame, selectedAlgo);
        compSolved = false;
        compStartMs = System.currentTimeMillis();
        compEndMs   = 0;
        compBoard.highlightPath = false;
        compBoard.setGame(compGame);
        compBoard.repaint();
        btnRunComp.setEnabled(true);
        btnRestartComp.setEnabled(false);
        btnAnalysis.setEnabled(false);
        setMessage("↺ Computer board reset  |  Algorithm: " + solver.getMetrics().algoName + "  |  Press ▶ Run Computer to solve.");
    }

    /** Allows changing the algorithm and immediately resets the computer board. */
    private void handleChangeAlgo() {
        if (compTimer != null && compTimer.isRunning()) compTimer.stop();

        String[] algos = {"Greedy", "Divide & Conquer", "Dynamic Programming", "Backtracking"};
        JComboBox<String> cbAlgo = new JComboBox<>(algos);
        cbAlgo.setSelectedIndex(selectedAlgo.ordinal());

        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        p.add(new JLabel("Select algorithm for Computer Solver:"), BorderLayout.NORTH);
        p.add(cbAlgo, BorderLayout.CENTER);

        JDialog dialog = new JDialog(this, "Change Algorithm", true);
        dialog.setLayout(new BorderLayout());
        dialog.add(p, BorderLayout.CENTER);

        JButton btnOK     = new JButton("Apply & Reset Computer");
        JButton btnCancel = new JButton("Cancel");
        final boolean[] ok = {false};
        btnOK    .addActionListener(e -> { ok[0] = true;  dialog.dispose(); });
        btnCancel.addActionListener(e -> dialog.dispose());

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.CENTER));
        btns.add(btnOK); btns.add(btnCancel);
        dialog.add(btns, BorderLayout.SOUTH);
        dialog.getRootPane().setDefaultButton(btnOK);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);

        if (!ok[0]) return;
        selectedAlgo = ComputerSolver.Algo.values()[cbAlgo.getSelectedIndex()];
        handleRestartComputer();   // reset board with new algo
    }
    private void startComputerSolve() {
        if (compTimer != null && compTimer.isRunning()) return;
        if (compSolved || solver == null) return;
        btnRunComp.setEnabled(false);
        btnRestartComp.setEnabled(true);
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
                btnRestartComp.setEnabled(true);
                return;
            }
            if (!moved) {
                compTimer.stop();
                setMessage("🤖 Computer finished — check path with Check button.");
                btnAnalysis.setEnabled(true);
                btnRestartComp.setEnabled(true);
            }
        });
        compTimer.start();
    }

    // ═════════════════════════════════════════════════════════════════════
    //  ANALYSIS DIALOG
    // ═════════════════════════════════════════════════════════════════════
    /** Shows the analysis dialog with performance metrics and complexity graphs. */
    private void showAnalysisDialog() {
        long uMs = userEndMs > 0 ? (userEndMs - userStartMs)
                                 : (System.currentTimeMillis() - userStartMs);
        long cMs = compEndMs > 0 ? (compEndMs - compStartMs)
                                 : (System.currentTimeMillis() - compStartMs);

        int N = setW * setH;

        // Populate the persistent panel (also records this run in sessionRuns)
        analysisPanel.populate(userMoves, uMs, userSolved,
                               solver, compSolved, cMs,
                               N, setW, setH);

        // Show the panel inside a scrollable dialog
        JScrollPane scroll = new JScrollPane(analysisPanel);
        scroll.setPreferredSize(new Dimension(560, 640));
        scroll.getVerticalScrollBar().setUnitIncrement(14);
        scroll.setBorder(null);

        JDialog dlg = new JDialog(this, "📊 Game Analysis", true);
        dlg.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dlg.setLayout(new BorderLayout());
        dlg.add(scroll, BorderLayout.CENTER);
        dlg.pack();
        dlg.setLocationRelativeTo(this);
        dlg.setVisible(true);
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
