package Tracks;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.util.List;

/**
 * AnalysisPanel — displays game results and algorithm performance.
 * Shows winner banner, stats cards, complexity table, and per-step log.
 */
public class AnalysisPanel extends JPanel {

    // UI colors
    private static final Color BG       = new Color(15, 23, 42);
    private static final Color CARD_BG  = new Color(30, 41, 59);
    private static final Color BORDER   = new Color(51, 65, 85);
    private static final Color FG_HEAD  = new Color(241, 245, 249);
    private static final Color FG_SUB   = new Color(148, 163, 184);
    private static final Color FG_DIM   = new Color(100, 116, 139);
    private static final Color GREEN    = new Color(34, 197, 94);
    private static final Color BLUE     = new Color(59, 130, 246);
    private static final Color AMBER    = new Color(251, 146, 60);
    private static final Color CYAN     = new Color(34, 211, 238);

    // Fonts
    private static final Font MONO_B  = new Font("Monospaced", Font.BOLD, 13);
    private static final Font MONO_SM = new Font("Monospaced", Font.PLAIN, 11);
    private static final Font SANS_B  = new Font("SansSerif", Font.BOLD, 14);
    private static final Font SANS    = new Font("SansSerif", Font.PLAIN, 12);

    public AnalysisPanel() {
        setBackground(BG);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
    }

    /** Populates the panel with game analysis data. */
    public void populate(int userMoves, long userTimeMs, boolean userSolved,
                         ComputerSolver solver, boolean compSolved, long compWallMs,
                         int boardN, int W, int H) {
        removeAll();

        AlgoMetrics am = solver.getMetrics();

        // Winner banner
        add(banner(userSolved, compSolved, userTimeMs, compWallMs, am.algoName));
        add(Box.createVerticalStrut(12));

        // Side-by-side stat cards
        JPanel cards = new JPanel(new GridLayout(1, 2, 12, 0));
        cards.setBackground(BG); cards.setMaximumSize(new Dimension(9999, 130));
        cards.add(statCard("👤  Player",  GREEN, userSolved, userMoves, userTimeMs,  "-"));
        cards.add(statCard("🤖  Computer ("+am.algoName+")", BLUE,
                           compSolved, solver.getTotalMoves(), compWallMs,
                           String.format("%,d ops", solver.getCumulativeOps())));
        add(cards);
        add(Box.createVerticalStrut(12));

        // Complexity analysis
        add(complexityCard(am, boardN, W, H, solver));
        add(Box.createVerticalStrut(12));

        // Per-step breakdown
        add(stepLogCard(solver.getStepLog()));
        add(Box.createVerticalStrut(8));

        revalidate(); repaint();
    }

    /** Builds the winner banner panel. */
    private JPanel banner(boolean userSolved, boolean compSolved,
                          long uMs, long cMs, String algoName) {
        JPanel p = card(null);
        p.setLayout(new BorderLayout());

        String title, who;
        Color wc;
        if (userSolved && compSolved) {
            if (uMs < cMs) { title = "🏆  YOU WIN!"; wc = GREEN; who = "Player was faster"; }
            else if (cMs < uMs) { title = "🤖  COMPUTER WINS"; wc = BLUE; who = algoName+" was faster"; }
            else { title = "🤝  TIE!"; wc = AMBER; who = "Exact same time"; }
        } else if (userSolved) { title = "🏆  YOU WIN!"; wc = GREEN; who = "Computer did not finish"; }
        else if (compSolved)   { title = "🤖  COMPUTER WINS"; wc = BLUE; who = "You did not finish"; }
        else                   { title = "⏱  GAME OVER"; wc = AMBER; who = "Neither side solved it"; }

        JLabel lbl = new JLabel(title, SwingConstants.CENTER);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 20));
        lbl.setForeground(wc);
        JLabel sub = new JLabel(who, SwingConstants.CENTER);
        sub.setFont(SANS); sub.setForeground(FG_SUB);
        p.add(lbl, BorderLayout.CENTER);
        p.add(sub, BorderLayout.SOUTH);
        p.setPreferredSize(new Dimension(0, 66));
        p.setMaximumSize(new Dimension(9999, 66));
        return p;
    }

    /** Builds a stat card with title and metrics. */
    private JPanel statCard(String title, Color accent, boolean solved,
                            int moves, long ms, String extra) {
        JPanel p = card(accent);
        p.setLayout(new GridLayout(5, 1, 0, 2));
        p.add(label(title, MONO_B, accent));
        p.add(row("Status", solved ? "✅ Solved" : "❌ Not solved", solved ? GREEN : AMBER));
        p.add(row("Moves",  String.valueOf(moves), FG_HEAD));
        p.add(row("Time",   String.format("%.2f s", ms / 1000.0), CYAN));
        p.add(row("Extra",  extra, FG_SUB));
        return p;
    }

    /** Builds the algorithm complexity analysis card. */
    private JPanel complexityCard(AlgoMetrics am, int N, int W, int H,
                                  ComputerSolver solver) {
        JPanel p = card(null);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        p.add(label("📊  Algorithm Analysis — " + am.algoName, SANS_B, FG_HEAD));
        p.add(Box.createVerticalStrut(6));
        p.add(row("Strategy",        am.strategyDesc,   FG_SUB));
        p.add(row("Time Complexity",  am.timeComplexity, CYAN));
        p.add(row("Space Complexity", am.spaceComplexity, AMBER));
        p.add(Box.createVerticalStrut(4));
        p.add(hRule());
        p.add(Box.createVerticalStrut(4));
        p.add(row("Board N (W×H)",       W+"×"+H+" = "+N+" cells", FG_SUB));
        p.add(row("Total Ops",           String.format("%,d", solver.getCumulativeOps()), CYAN));
        p.add(row("Total Algo Time",      am.totalTimeMs(), CYAN));
        p.add(row("Avg Time / Step",
                  solver.getTotalMoves() == 0 ? "—" :
                  String.format("%.3f ms", am.totalTimeNs / 1_000_000.0 / solver.getTotalMoves()),
                  FG_HEAD));
        p.add(row("Last Step Space",     am.spaceUsed+" cells/entries", FG_SUB));
        return p;
    }

    /** Builds the per-step breakdown table. */
    private JPanel stepLogCard(List<long[]> log) {
        JPanel p = card(null);
        p.setLayout(new BorderLayout());
        p.add(label("📋  Per-Step Breakdown (last 12 steps)", SANS_B, FG_HEAD), BorderLayout.NORTH);

        String[] cols = {"Step", "Time (ms)", "Ops", "Space"};
        int show = Math.min(12, log.size());
        Object[][] data = new Object[show][4];
        for (int i = 0; i < show; i++) {
            long[] s = log.get(log.size() - show + i);
            data[i][0] = "#" + (log.size() - show + i + 1);
            data[i][1] = String.format("%.3f", s[0] / 1_000_000.0);
            data[i][2] = s[1];
            data[i][3] = s[2];
        }

        JTable table = new JTable(data, cols) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table.setBackground(CARD_BG);
        table.setForeground(FG_HEAD);
        table.setFont(MONO_SM);
        table.getTableHeader().setBackground(new Color(15, 23, 42));
        table.getTableHeader().setForeground(FG_DIM);
        table.getTableHeader().setFont(MONO_SM);
        table.setRowHeight(20);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(8, 2));
        table.setSelectionBackground(new Color(30, 64, 120));
        JScrollPane sp = new JScrollPane(table);
        sp.setBackground(CARD_BG);
        sp.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        sp.setPreferredSize(new Dimension(0, 160));
        p.add(sp, BorderLayout.CENTER);
        return p;
    }

    // --- Helper methods ---

    /** Creates a card panel with optional accent border. */
    private JPanel card(Color accentLeft) {
        JPanel p = new JPanel();
        p.setBackground(CARD_BG);
        p.setBorder(BorderFactory.createCompoundBorder(
            accentLeft != null
                ? BorderFactory.createMatteBorder(0, 3, 0, 0, accentLeft)
                : BorderFactory.createLineBorder(BORDER),
            BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));
        return p;
    }

    /** Creates a styled label. */
    private JLabel label(String text, Font f, Color c) {
        JLabel l = new JLabel(text);
        l.setFont(f); l.setForeground(c);
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }

    /** Creates a key-value row. */
    private JPanel row(String key, String val, Color valColor) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(CARD_BG);
        JLabel kl = new JLabel(key); kl.setFont(MONO_SM); kl.setForeground(FG_DIM);
        JLabel vl = new JLabel(val); vl.setFont(MONO_SM); vl.setForeground(valColor);
        p.add(kl, BorderLayout.WEST); p.add(vl, BorderLayout.EAST);
        p.setMaximumSize(new Dimension(9999, 20));
        return p;
    }

    /** Creates a horizontal separator line. */
    private JSeparator hRule() {
        JSeparator sep = new JSeparator();
        sep.setForeground(BORDER); sep.setBackground(CARD_BG);
        sep.setMaximumSize(new Dimension(9999, 1));
        return sep;
    }
}
