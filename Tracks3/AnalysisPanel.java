package Tracks;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

/**
 * AnalysisPanel — displays game results and algorithm performance.
 *
 * Features:
 *  - sessionRuns accumulates every algorithm run within a game (reset only on newGame()).
 *  - Two graph buttons: "⏱ Time Complexity" and "💾 Space Complexity".
 *  - Each button opens a window with:
 *      TOP chart    → per-step LINE graph (one coloured line per algorithm run)
 *      BOTTOM chart → whole-game BAR graph (total time or total space per run)
 *  - Legend at the bottom of the graph window lists every algorithm run with colour dot.
 *  - Graphs persist across algorithm changes within the same game.
 *  - Graphs reset only when New Game starts (newGame() is called).
 */
public class AnalysisPanel extends JPanel {

    // ── UI Colors (light / white theme) ──────────────────────────────────────
    private static final Color BG      = new Color(245, 247, 250);   // light grey page bg
    private static final Color CARD_BG = Color.WHITE;                // white cards
    private static final Color BORDER  = new Color(209, 213, 219);   // light border
    private static final Color FG_HEAD = new Color(17,  24,  39);    // near-black heading
    private static final Color FG_SUB  = new Color(75,  85,  99);    // dark-grey sub text
    private static final Color FG_DIM  = new Color(156, 163, 175);   // muted label
    private static final Color GREEN   = new Color(22,  163, 74);    // vivid green
    private static final Color BLUE    = new Color(37,  99,  235);   // vivid blue
    private static final Color AMBER   = new Color(217, 119, 6);     // vivid amber
    private static final Color CYAN    = new Color(8,   145, 178);   // vivid cyan

    // ── Fonts ─────────────────────────────────────────────────────────────────
    private static final Font MONO_B  = new Font("Monospaced", Font.BOLD, 13);
    private static final Font MONO_SM = new Font("Monospaced", Font.PLAIN, 11);
    private static final Font SANS_B  = new Font("SansSerif", Font.BOLD, 14);
    private static final Font SANS    = new Font("SansSerif", Font.PLAIN, 12);

    // ── Per-algorithm colour palette (cycles if > 6 algos) ───────────────────
    private static final Color[] ALGO_COLORS = {
        new Color(59,  130, 246),   // blue
        new Color(34,  197, 94),    // green
        new Color(251, 146, 60),    // amber
        new Color(168, 85,  247),   // purple
        new Color(236, 72,  153),   // pink
        new Color(34,  211, 238),   // cyan
    };

    // ────────────────────────────────────────────────────────────────────────────
    //  AlgoRun — snapshot of one algorithm execution within the current game
    // ────────────────────────────────────────────────────────────────────────────
    public static class AlgoRun {
        final String       algoName;
        final int          moves;
        final long         wallMs;
        final long         totalOps;
        final long         totalTimeNs;
        final long         spaceUsed;
        final boolean      solved;
        final List<long[]> stepLog;   // each entry: [timeNs, ops, space]

        AlgoRun(String algoName, int moves, long wallMs, long totalOps,
                long totalTimeNs, long spaceUsed, boolean solved,
                List<long[]> stepLog) {
            this.algoName    = algoName;
            this.moves       = moves;
            this.wallMs      = wallMs;
            this.totalOps    = totalOps;
            this.totalTimeNs = totalTimeNs;
            this.spaceUsed   = spaceUsed;
            this.solved      = solved;
            this.stepLog     = new ArrayList<>(stepLog);
        }
    }

    // ── Session history — persists across algorithm changes, cleared on newGame() ──
    private final List<AlgoRun> sessionRuns = new ArrayList<>();

    /**
     * Called from TracksGame.startNewGame() to reset graph history for a fresh game.
     * This is the ONLY time graphs are cleared.
     */
    public void newGame() {
        sessionRuns.clear();
    }

    // ────────────────────────────────────────────────────────────────────────────
    //  Constructor
    // ────────────────────────────────────────────────────────────────────────────
    public AnalysisPanel() {
        setBackground(BG);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
    }

    // ────────────────────────────────────────────────────────────────────────────
    //  populate() — called after each round ends
    // ────────────────────────────────────────────────────────────────────────────
    public void populate(int userMoves, long userTimeMs, boolean userSolved,
                         ComputerSolver solver, boolean compSolved, long compWallMs,
                         int boardN, int W, int H) {
        removeAll();
        AlgoMetrics am = solver.getMetrics();

        // Save this run into session history (persists until newGame())
        sessionRuns.add(new AlgoRun(
            am.algoName,
            solver.getTotalMoves(),
            compWallMs,
            solver.getCumulativeOps(),
            am.totalTimeNs,
            am.spaceUsed,
            compSolved,
            solver.getStepLog()
        ));

        // ── Winner banner ────────────────────────────────────────────────────
        add(banner(userSolved, compSolved, userTimeMs, compWallMs, am.algoName));
        add(Box.createVerticalStrut(12));

        // ── Side-by-side stat cards ──────────────────────────────────────────
        JPanel cards = new JPanel(new GridLayout(1, 2, 12, 0));
        cards.setBackground(BG);
        cards.setMaximumSize(new Dimension(9999, 130));
        cards.add(statCard("👤  Player", GREEN, userSolved, userMoves, userTimeMs, "-"));
        cards.add(statCard("🤖  Computer (" + am.algoName + ")", BLUE,
                compSolved, solver.getTotalMoves(), compWallMs,
                String.format("%,d ops", solver.getCumulativeOps())));
        add(cards);
        add(Box.createVerticalStrut(12));

        // ── Complexity analysis card ─────────────────────────────────────────
        add(complexityCard(am, boardN, W, H, solver));
        add(Box.createVerticalStrut(12));

        // ── Graph buttons ────────────────────────────────────────────────────
        add(graphButtonPanel());
        add(Box.createVerticalStrut(12));

        // ── Per-step breakdown table ─────────────────────────────────────────
        add(stepLogCard(solver.getStepLog()));
        add(Box.createVerticalStrut(8));

        revalidate();
        repaint();
    }

    // ────────────────────────────────────────────────────────────────────────────
    //  Graph button panel
    // ────────────────────────────────────────────────────────────────────────────
    private JPanel graphButtonPanel() {
        JPanel p = card(null);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setMaximumSize(new Dimension(9999, 115));

        int runs = sessionRuns.size();
        p.add(label("📈  Complexity Graphs  (" + runs + " algorithm run" +
                (runs == 1 ? "" : "s") + " this game)", SANS_B, FG_HEAD));
        p.add(Box.createVerticalStrut(3));
        p.add(label("Graphs accumulate every algorithm run — reset only when New Game starts",
                MONO_SM, FG_SUB));
        p.add(Box.createVerticalStrut(10));

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        btns.setBackground(CARD_BG);

        JButton timeBtn  = graphButton("⏱  Time Complexity",  CYAN);
        JButton spaceBtn = graphButton("💾  Space Complexity", AMBER);

        timeBtn .addActionListener(e -> showGraphWindow("time"));
        spaceBtn.addActionListener(e -> showGraphWindow("space"));

        btns.add(timeBtn);
        btns.add(spaceBtn);
        p.add(btns);
        return p;
    }

    /** Styled borderless button with coloured outline and hover glow. */
    private JButton graphButton(String text, Color accent) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                int alpha = getModel().isPressed() ? 90
                          : getModel().isRollover() ? 55 : 22;
                g2.setColor(new Color(accent.getRed(), accent.getGreen(),
                        accent.getBlue(), alpha));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(accent);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        btn.setForeground(accent);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(192, 36));
        return btn;
    }

    // ────────────────────────────────────────────────────────────────────────────
    //  Graph window — two charts stacked (line on top, bar on bottom)
    // ────────────────────────────────────────────────────────────────────────────
    private void showGraphWindow(String mode) {
        if (sessionRuns.isEmpty()) return;

        JFrame frame = new JFrame(mode.equals("time")
                ? "⏱  Time Complexity — All Algorithms This Game"
                : "💾  Space Complexity — All Algorithms This Game");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(920, 720);
        frame.setMinimumSize(new Dimension(700, 500));
        frame.setLocationRelativeTo(this);

        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(BG);

        // ── Header bar ───────────────────────────────────────────────────────
        JLabel hdr = new JLabel(mode.equals("time")
                ? "  ⏱  Time per Step (ms)  ·  Total Execution Time — All Algorithms This Game"
                : "  💾  Space per Step  ·  Total Space Used — All Algorithms This Game");
        hdr.setFont(new Font("SansSerif", Font.BOLD, 14));
        hdr.setForeground(FG_HEAD);
        hdr.setOpaque(true);
        hdr.setBackground(CARD_BG);
        hdr.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        root.add(hdr, BorderLayout.NORTH);

        // ── Two charts ───────────────────────────────────────────────────────
        JPanel charts = new JPanel(new GridLayout(2, 1, 0, 10));
        charts.setBackground(BG);
        charts.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        charts.add(lineChartPanel(mode));   // TOP  — per-step lines
        charts.add(barChartPanel(mode));    // BOTTOM — whole-game bars
        root.add(charts, BorderLayout.CENTER);

        // ── Legend ───────────────────────────────────────────────────────────
        root.add(legendPanel(), BorderLayout.SOUTH);

        frame.setContentPane(root);
        frame.setVisible(true);
    }

    // ────────────────────────────────────────────────────────────────────────────
    //  TOP CHART — per-step line graph, one line per AlgoRun
    // ────────────────────────────────────────────────────────────────────────────
    private JPanel lineChartPanel(final String mode) {
        return new JPanel() {
            {
                setBackground(CARD_BG);
                setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(BORDER),
                        BorderFactory.createEmptyBorder(14, 14, 10, 14)));
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                        RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                int W = getWidth(), H = getHeight();
                int ox = 66, oy = 28;
                int plotW = W - ox - 18;
                int plotH = H - oy - 38;
                int baseY = oy + plotH;

                // Chart title
                g2.setFont(new Font("SansSerif", Font.BOLD, 12));
                g2.setColor(FG_HEAD);
                g2.drawString(mode.equals("time")
                        ? "Step-by-Step  Time (ms) per Algorithm"
                        : "Step-by-Step  Space Used per Algorithm",
                        ox, oy - 10);

                // Find global max value and max step count across all runs
                long maxVal  = 1;
                int  maxStep = 1;
                for (AlgoRun r : sessionRuns) {
                    maxStep = Math.max(maxStep, r.stepLog.size());
                    for (long[] s : r.stepLog) {
                        long v = mode.equals("time") ? s[0] : s[2];
                        maxVal = Math.max(maxVal, v);
                    }
                }

                // Grid lines + Y-axis labels
                for (int i = 0; i <= 5; i++) {
                    int  y  = baseY - i * plotH / 5;
                    long lv = maxVal * i / 5;
                    String lbl = mode.equals("time")
                            ? String.format("%.2f", lv / 1_000_000.0)
                            : String.valueOf(lv);
                    g2.setFont(new Font("Monospaced", Font.PLAIN, 9));
                    g2.setColor(FG_DIM);
                    g2.drawString(lbl, 2, y + 4);
                    g2.setColor(new Color(51, 65, 85, 100));
                    g2.setStroke(new BasicStroke(0.6f, BasicStroke.CAP_BUTT,
                            BasicStroke.JOIN_ROUND, 0, new float[]{4, 4}, 0));
                    g2.drawLine(ox, y, ox + plotW, y);
                }

                // Axes
                g2.setColor(BORDER);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawLine(ox, oy, ox, baseY);
                g2.drawLine(ox, baseY, ox + plotW, baseY);

                // Y-axis unit + X-axis label
                g2.setFont(new Font("Monospaced", Font.PLAIN, 9));
                g2.setColor(FG_DIM);
                g2.drawString(mode.equals("time") ? "ms" : "cells", 2, oy + 4);
                g2.drawString("Step →", ox + plotW - 36, baseY + 16);

                // X-axis step tick labels (up to 10 ticks)
                int tickEvery = Math.max(1, maxStep / 10);
                for (int si = 0; si < maxStep; si += tickEvery) {
                    int px = ox + (maxStep == 1 ? plotW / 2
                                 : si * plotW / (maxStep - 1));
                    g2.setColor(FG_DIM);
                    g2.drawString(String.valueOf(si + 1), px - 3, baseY + 14);
                }

                // Draw one coloured line per algorithm run
                for (int ri = 0; ri < sessionRuns.size(); ri++) {
                    AlgoRun      run   = sessionRuns.get(ri);
                    Color        color = ALGO_COLORS[ri % ALGO_COLORS.length];
                    List<long[]> log   = run.stepLog;
                    if (log.isEmpty()) continue;

                    int steps = log.size();
                    g2.setStroke(new BasicStroke(2.4f,
                            BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.setColor(color);

                    int prevX = -1, prevY = -1;
                    for (int si = 0; si < steps; si++) {
                        long v  = mode.equals("time") ? log.get(si)[0] : log.get(si)[2];
                        int  px = ox + (steps == 1 ? plotW / 2
                                      : si * plotW / (steps - 1));
                        int  py = baseY - (int)((double) v / maxVal * plotH);

                        if (prevX >= 0) g2.drawLine(prevX, prevY, px, py);
                        g2.fillOval(px - 3, py - 3, 7, 7);
                        prevX = px; prevY = py;
                    }

                    // Algorithm name label near last data point
                    long lastV = mode.equals("time")
                            ? log.get(steps - 1)[0] : log.get(steps - 1)[2];
                    int lastX = ox + (steps == 1 ? plotW / 2 : plotW);
                    int lastY = baseY - (int)((double) lastV / maxVal * plotH);
                    g2.setFont(new Font("SansSerif", Font.BOLD, 9));
                    g2.drawString(run.algoName,
                            Math.min(lastX + 4, ox + plotW - 55),
                            Math.max(oy + 12, lastY - 2));
                }
            }
        };
    }

    // ────────────────────────────────────────────────────────────────────────────
    //  BOTTOM CHART — whole-game bar graph, one bar per AlgoRun
    // ────────────────────────────────────────────────────────────────────────────
    private JPanel barChartPanel(final String mode) {
        return new JPanel() {
            {
                setBackground(CARD_BG);
                setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(BORDER),
                        BorderFactory.createEmptyBorder(14, 14, 10, 14)));
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                        RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                int W = getWidth(), H = getHeight();
                int ox = 66, oy = 28;
                int plotW = W - ox - 18;
                int plotH = H - oy - 42;
                int baseY = oy + plotH;

                // Chart title
                g2.setFont(new Font("SansSerif", Font.BOLD, 12));
                g2.setColor(FG_HEAD);
                g2.drawString(mode.equals("time")
                        ? "Total Execution Time (s) per Algorithm"
                        : "Total Space Used per Algorithm",
                        ox, oy - 10);

                int n = sessionRuns.size();
                if (n == 0) return;

                // Collect total values for each run
                long[] vals = new long[n];
                long   maxV = 1;
                for (int i = 0; i < n; i++) {
                    AlgoRun r = sessionRuns.get(i);
                    vals[i] = mode.equals("time") ? r.wallMs : r.spaceUsed;
                    maxV    = Math.max(maxV, vals[i]);
                }

                // Grid lines + Y-axis labels
                for (int i = 0; i <= 5; i++) {
                    int  y  = baseY - i * plotH / 5;
                    long lv = maxV * i / 5;
                    String lbl = mode.equals("time")
                            ? String.format("%.2fs", lv / 1000.0)
                            : String.valueOf(lv);
                    g2.setFont(new Font("Monospaced", Font.PLAIN, 9));
                    g2.setColor(FG_DIM);
                    g2.drawString(lbl, 2, y + 4);
                    g2.setColor(new Color(51, 65, 85, 100));
                    g2.setStroke(new BasicStroke(0.6f, BasicStroke.CAP_BUTT,
                            BasicStroke.JOIN_ROUND, 0, new float[]{4, 4}, 0));
                    g2.drawLine(ox, y, ox + plotW, y);
                }

                // Axes
                g2.setColor(BORDER);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawLine(ox, oy, ox, baseY);
                g2.drawLine(ox, baseY, ox + plotW, baseY);

                // Draw bars
                int slotW = plotW / n;
                int barW  = Math.max(20, slotW - 22);

                for (int i = 0; i < n; i++) {
                    Color color = ALGO_COLORS[i % ALGO_COLORS.length];
                    int   bx    = ox + i * slotW + (slotW - barW) / 2;
                    int   bh    = (int)((double) vals[i] / maxV * plotH);
                    int   by    = baseY - bh;

                    // Gradient fill
                    GradientPaint gp = new GradientPaint(
                            bx, by, color,
                            bx, baseY, color.darker().darker());
                    g2.setPaint(gp);
                    g2.fillRoundRect(bx, by, barW, Math.max(bh, 2), 6, 6);

                    // Outline
                    g2.setColor(color.brighter());
                    g2.setStroke(new BasicStroke(1f));
                    g2.drawRoundRect(bx, by, barW, Math.max(bh, 2), 6, 6);

                    // Value label on top of bar
                    String valStr = mode.equals("time")
                            ? String.format("%.2fs", vals[i] / 1000.0)
                            : String.valueOf(vals[i]);
                    g2.setFont(new Font("Monospaced", Font.BOLD, 9));
                    g2.setColor(FG_HEAD);
                    FontMetrics fmv = g2.getFontMetrics();
                    g2.drawString(valStr,
                            bx + (barW - fmv.stringWidth(valStr)) / 2,
                            Math.max(oy + 13, by - 3));

                    // Algorithm name below bar
                    g2.setFont(new Font("SansSerif", Font.BOLD, 10));
                    g2.setColor(color);
                    FontMetrics fmn = g2.getFontMetrics();
                    String name = sessionRuns.get(i).algoName;
                    g2.drawString(name,
                            bx + (barW - fmn.stringWidth(name)) / 2,
                            baseY + 14);

                    // Solved / not-solved badge
                    g2.setFont(new Font("SansSerif", Font.BOLD, 11));
                    g2.setColor(sessionRuns.get(i).solved ? GREEN : AMBER);
                    String badge = sessionRuns.get(i).solved ? "✓" : "✗";
                    g2.drawString(badge,
                            bx + (barW - fmn.stringWidth(badge)) / 2,
                            baseY + 27);
                }
            }
        };
    }

    // ────────────────────────────────────────────────────────────────────────────
    //  Legend panel at bottom of graph window
    // ────────────────────────────────────────────────────────────────────────────
    private JPanel legendPanel() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 8));
        p.setBackground(new Color(243, 244, 246));
        p.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER));

        JLabel hdr = new JLabel("Algorithms this game: ");
        hdr.setFont(new Font("SansSerif", Font.BOLD, 11));
        hdr.setForeground(FG_DIM);
        p.add(hdr);

        for (int i = 0; i < sessionRuns.size(); i++) {
            AlgoRun run   = sessionRuns.get(i);
            Color   color = ALGO_COLORS[i % ALGO_COLORS.length];

            // Coloured circle dot
            final Color fc = color;
            JPanel dot = new JPanel() {
                { setPreferredSize(new Dimension(11, 11)); setOpaque(false); }
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(fc);
                    g2.fillOval(0, 0, 11, 11);
                }
            };

            JLabel lbl = new JLabel(
                    run.algoName + "  " +
                    run.stepLog.size() + " steps  " +
                    (run.solved ? "✅" : "❌") + "  " +
                    String.format("%.2fs", run.wallMs / 1000.0));
            lbl.setFont(new Font("Monospaced", Font.PLAIN, 11));
            lbl.setForeground(color);

            JPanel chip = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
            chip.setOpaque(false);
            chip.add(dot);
            chip.add(lbl);
            p.add(chip);
        }
        return p;
    }

    // ────────────────────────────────────────────────────────────────────────────
    //  Original panel sections
    // ────────────────────────────────────────────────────────────────────────────

    private JPanel banner(boolean userSolved, boolean compSolved,
                          long uMs, long cMs, String algoName) {
        JPanel p = card(null);
        p.setLayout(new BorderLayout());

        String title, who; Color wc;
        if (userSolved && compSolved) {
            if      (uMs < cMs) { title = "🏆  YOU WIN!";      wc = GREEN; who = "Player was faster"; }
            else if (cMs < uMs) { title = "🤖  COMPUTER WINS"; wc = BLUE;  who = algoName + " was faster"; }
            else                { title = "🤝  TIE!";          wc = AMBER; who = "Exact same time"; }
        } else if (userSolved)  { title = "🏆  YOU WIN!";      wc = GREEN; who = "Computer did not finish"; }
        else if (compSolved)    { title = "🤖  COMPUTER WINS"; wc = BLUE;  who = "You did not finish"; }
        else                    { title = "⏱  GAME OVER";      wc = AMBER; who = "Neither side solved it"; }

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

    private JPanel complexityCard(AlgoMetrics am, int N, int W, int H,
                                  ComputerSolver solver) {
        JPanel p = card(null);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.add(label("📊  Algorithm Analysis — " + am.algoName, SANS_B, FG_HEAD));
        p.add(Box.createVerticalStrut(6));
        p.add(row("Strategy",        am.strategyDesc,    FG_SUB));
        p.add(row("Time Complexity",  am.timeComplexity,  CYAN));
        p.add(row("Space Complexity", am.spaceComplexity, AMBER));
        p.add(Box.createVerticalStrut(4));
        p.add(hRule());
        p.add(Box.createVerticalStrut(4));
        p.add(row("Board N (W×H)",   W + "×" + H + " = " + N + " cells", FG_SUB));
        p.add(row("Total Ops",       String.format("%,d", solver.getCumulativeOps()), CYAN));
        p.add(row("Total Algo Time", am.totalTimeMs(), CYAN));
        p.add(row("Avg Time / Step",
                solver.getTotalMoves() == 0 ? "—" :
                String.format("%.3f ms", am.totalTimeNs / 1_000_000.0 / solver.getTotalMoves()),
                FG_HEAD));
        p.add(row("Last Step Space", am.spaceUsed + " cells/entries", FG_SUB));
        return p;
    }

    private JPanel stepLogCard(List<long[]> log) {
        JPanel p = card(null);
        p.setLayout(new BorderLayout());
        p.add(label("📋  Per-Step Breakdown (last 12 steps)", SANS_B, FG_HEAD),
                BorderLayout.NORTH);

        String[]   cols = {"Step", "Time (ms)", "Ops", "Space"};
        int        show = Math.min(12, log.size());
        Object[][] data = new Object[show][4];
        for (int i = 0; i < show; i++) {
            long[] s   = log.get(log.size() - show + i);
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
        table.getTableHeader().setBackground(new Color(243, 244, 246));
        table.getTableHeader().setForeground(FG_SUB);
        table.getTableHeader().setFont(MONO_SM);
        table.setRowHeight(20);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(8, 2));
        table.setSelectionBackground(new Color(219, 234, 254));

        JScrollPane sp = new JScrollPane(table);
        sp.setBackground(CARD_BG);
        sp.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        sp.setPreferredSize(new Dimension(0, 160));
        p.add(sp, BorderLayout.CENTER);
        return p;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private JPanel card(Color accentLeft) {
        JPanel p = new JPanel();
        p.setBackground(CARD_BG);
        p.setBorder(BorderFactory.createCompoundBorder(
                accentLeft != null
                        ? BorderFactory.createMatteBorder(0, 3, 0, 0, accentLeft)
                        : BorderFactory.createLineBorder(new Color(229, 231, 235)),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));
        return p;
    }

    private JLabel label(String text, Font f, Color c) {
        JLabel l = new JLabel(text);
        l.setFont(f); l.setForeground(c);
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }

    private JPanel row(String key, String val, Color valColor) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(CARD_BG);
        JLabel kl = new JLabel(key); kl.setFont(MONO_SM); kl.setForeground(FG_SUB);
        JLabel vl = new JLabel(val); vl.setFont(MONO_SM); vl.setForeground(valColor);
        p.add(kl, BorderLayout.WEST);
        p.add(vl, BorderLayout.EAST);
        p.setMaximumSize(new Dimension(9999, 20));
        return p;
    }

    private JSeparator hRule() {
        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(229, 231, 235));
        sep.setBackground(Color.WHITE);
        sep.setMaximumSize(new Dimension(9999, 1));
        return sep;
    }
}
