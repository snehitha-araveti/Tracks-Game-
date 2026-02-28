package Tracks;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

/**
 * BoardPanel — renders the game board and handles user input.
 * White background, dark-gray grid, light-green clues,
 * blue "A" start, red "B" end, yellow path highlight.
 *
 * @author [Your Name]
 */
public class BoardPanel extends JPanel {

    private int startX, startY;   // Board offset for centering
    private Game g;               // Reference to game state
    private int margin   = 40;    // Margin around board
    private int cellSize = 50;   // Size of each cell
    public  boolean highlightPath = false;

    /** Listener for move events */
    public interface MoveListener { void onMove(int x, int y); }
    private MoveListener moveListener;
    private final boolean interactive;

    /** Creates board panel with optional mouse interaction. */
    public BoardPanel(boolean interactive) {
        this.interactive = interactive;
        setBackground(Color.WHITE);
        if (interactive) {
            addMouseListener(new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) { handleMouseClick(e); }
            });
        }
    }

    /** Sets the move listener callback. */
    public void setMoveListener(MoveListener l) { this.moveListener = l; }

    /** Sets the game and recomputes panel size. */
    public void setGame(Game gg) {
        this.g = gg;
        highlightPath = false;
        computeSize();
        repaint();
    }

    /** Calculates cell size based on board dimensions. */
    private void computeSize() {
        if (g == null) return;
        int cs = Math.min(50, Math.max(24, 600 / Math.max(g.w, g.h)));
        cellSize = cs;
        margin   = 40;
        setPreferredSize(new Dimension(margin * 2 + g.w * cellSize,
                                       margin * 2 + g.h * cellSize));
        revalidate();
    }

    /** Handles mouse clicks for placing/removing pieces. */
    private void handleMouseClick(MouseEvent e) {
        if (g == null) return;
        int x = (e.getX() - startX) / cellSize;
        int y = (e.getY() - startY) / cellSize;
        if (x < 0 || x >= g.w || y < 0 || y >= g.h) return;

        // Left click: place/cycle piece
        if (SwingUtilities.isLeftMouseButton(e)) {
            if (moveListener != null) moveListener.onMove(x, y);
        }
        // Right click: remove piece (if not a clue)
        else if (SwingUtilities.isRightMouseButton(e)) {
            if (g.board[y][x].clue) return;
            g.hist.push(new Move(x, y, g.board[y][x].t, false));
            g.board[y][x].t = TType.EMPTY;
            highlightPath = false;
            g.rebuildGraph();
            repaint();
        }
    }

    @Override
    public void paintComponent(Graphics gg) {
        super.paintComponent(gg);
        if (g == null) return;

        // Calculate board offset for centering
        int boardW = g.w * cellSize;
        int boardH = g.h * cellSize;
        startX = (getWidth()  - boardW) / 2;
        startY = (getHeight() - boardH) / 2;

        Graphics2D g2 = (Graphics2D) gg;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setStroke(new BasicStroke(3));

        drawGrid(g2);
        drawPieces(g2);
        drawHighlightPath(g2);
        drawClues(g2);
    }

    /** Draws the grid background. */
    private void drawGrid(Graphics2D g2) {
        for (int y = 0; y < g.h; y++) {
            for (int x = 0; x < g.w; x++) {
                int sx = startX + x * cellSize;
                int sy = startY + y * cellSize;
                g2.setColor(new Color(245, 245, 245));
                g2.fillRect(sx, sy, cellSize, cellSize);
                g2.setColor(Color.DARK_GRAY);
                g2.drawRect(sx, sy, cellSize, cellSize);
            }
        }
    }

    /** Draws all track pieces on the board. */
    private void drawPieces(Graphics2D g2) {
        for (int y = 0; y < g.h; y++) {
            for (int x = 0; x < g.w; x++) {
                int cx = startX + x * cellSize + cellSize / 2;
                int cy = startY + y * cellSize + cellSize / 2;
                TType t = g.board[y][x].t;

                // Highlight clue cells
                if (g.board[y][x].clue) {
                    g2.setColor(new Color(220, 255, 220));
                    g2.fillRect(startX + x * cellSize + 2,
                                startY + y * cellSize + 2,
                                cellSize - 3, cellSize - 3);
                }

                // Draw track based on type
                g2.setColor(Color.BLACK);
                int half = cellSize / 2 - 8;
                switch (t) {
                    case HR:
                        g2.drawLine(cx - half, cy, cx + half, cy);
                        break;
                    case VY:
                        g2.drawLine(cx, cy - half, cx, cy + half);
                        break;
                    case NE:
                        g2.drawLine(cx, cy - half, cx, cy);
                        g2.drawLine(cx, cy, cx + half, cy);
                        g2.drawArc(cx - 8, cy - half - 8, half * 2, half * 2, 270, 90);
                        break;
                    case NW:
                        g2.drawLine(cx, cy - half, cx, cy);
                        g2.drawLine(cx - half, cy, cx, cy);
                        g2.drawArc(cx - half - 8, cy - half - 8, half * 2, half * 2, 180, 90);
                        break;
                    case SE:
                        g2.drawLine(cx, cy, cx + half, cy);
                        g2.drawLine(cx, cy, cx, cy + half);
                        g2.drawArc(cx - 8, cy - 8, half * 2, half * 2, 0, 90);
                        break;
                    case SW:
                        g2.drawLine(cx - half, cy, cx, cy);
                        g2.drawLine(cx, cy, cx, cy + half);
                        g2.drawArc(cx - half - 8, cy - 8, half * 2, half * 2, 90, 90);
                        break;
                    default:
                        break;
                }

                // Draw start marker (A)
                if (g.board[y][x].start) {
                    g2.setColor(Color.BLUE);
                    g2.fillOval(cx - 10, cy - 10, 20, 20);
                    g2.setColor(Color.WHITE);
                    g2.drawString("A", cx - 4, cy + 5);
                }
                // Draw end marker (B)
                if (g.board[y][x].end) {
                    g2.setColor(Color.RED);
                    g2.fillOval(cx - 10, cy - 10, 20, 20);
                    g2.setColor(Color.WHITE);
                    g2.drawString("B", cx - 4, cy + 5);
                }
            }
        }
    }

    /** Draws yellow highlight over the current path. */
    private void drawHighlightPath(Graphics2D g2) {
        if (!highlightPath) return;
        List<int[]> p = g.findPathFromCurrent();
        if (p == null) return;
        g2.setColor(new Color(255, 200, 0, 140));
        for (int[] c : p) {
            int sx = startX + c[0] * cellSize;
            int sy = startY + c[1] * cellSize;
            g2.fillRect(sx + 2, sy + 2, cellSize - 3, cellSize - 3);
        }
    }

    /** Draws row and column clue numbers. */
    private void drawClues(Graphics2D g2) {
        int[] colCnt = new int[g.w], rowCnt = new int[g.h];
        for (int y = 0; y < g.h; y++)
            for (int x = 0; x < g.w; x++)
                if (g.sol[y][x] != TType.EMPTY) { colCnt[x]++; rowCnt[y]++; }

        g2.setColor(Color.BLACK);
        // Column clues at top
        for (int x = 0; x < g.w; x++) {
            String s = "" + colCnt[x];
            int tx = startX + x * cellSize + cellSize / 2 - 6;
            g2.drawString(s, tx, startY - 12);
        }
        // Row clues at left
        for (int y = 0; y < g.h; y++) {
            String s = "" + rowCnt[y];
            int ty = startY + y * cellSize + cellSize / 2 + 4;
            g2.drawString(s, startX - 25, ty);
        }
    }
}
