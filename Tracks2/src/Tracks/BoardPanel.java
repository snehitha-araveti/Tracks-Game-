package Tracks;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

public class BoardPanel extends JPanel {
    private int startX, startY;
    private Game g;
    private int margin = 40;
    private int cellSize = 50;
    public boolean highlightPath = false;
    private TracksGame parent;

    public BoardPanel(TracksGame parent) {
        this.parent = parent;
        setBackground(Color.WHITE);
        addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                handleMouseClick(e);
            }
        });
    }

    private void handleMouseClick(MouseEvent e) {
        if (g == null) {
            return;
        }
        int x = (e.getX() - startX) / cellSize;
        int y = (e.getY() - startY) / cellSize;
        if (x < 0 || x >= g.w || y < 0 || y >= g.h) {
            return;
        }

        if (g.revealedSolution) {
            parent.setMessage("Restart to play again");
            return;
        }

        if (g.checkSolved()) {
            highlightPath = true;
            repaint();
            parent.setMessage("Solved!");
            return;
        }

        if (SwingUtilities.isLeftMouseButton(e)) {
            if (g.board[y][x].clue) {
                parent.setMessage("Cell is a clue (locked)");
                return;
            }

            g.hist.push(new Move(x, y, g.board[y][x].t, g.board[y][x].clue));
            g.board[y][x].t = Util.nextType(g.board[y][x].t);

            highlightPath = false;
            g.rebuildGraph();
            repaint();

            SwingUtilities.invokeLater(() -> {
                if (!g.checkSolved() && !g.revealedSolution) {
                    g.computerMove();
                }

                repaint();

                if (g.checkSolved()) {
                    parent.setMessage("Solved!");
                    highlightPath = true;
                    repaint();
                } else {
                    parent.setMessage("Player moved. Computer moved.");
                }
            });

        } else if (SwingUtilities.isRightMouseButton(e)) {
            if (g.board[y][x].clue) {
                parent.setMessage("Cell is a clue (locked)");
                return;
            }
            g.hist.push(new Move(x, y, g.board[y][x].t, g.board[y][x].clue));
            g.board[y][x].t = TType.EMPTY;

            highlightPath = false;
            g.rebuildGraph();
            repaint();
        }
    }

    public void setGame(Game gg) {
        this.g = gg;
        computeSize();
    }

    private void computeSize() {
        if (g == null) {
            return;
        }
        int cs = Math.min(50, Math.max(24, 600 / Math.max(g.w, g.h)));
        cellSize = cs;
        margin = 40;
        setPreferredSize(new Dimension(margin * 2 + g.w * cellSize, margin * 2 + g.h * cellSize));
        revalidate();
    }

    public void paintComponent(Graphics gg) {
        super.paintComponent(gg);
        if (g == null) {
            return;
        }

        int boardW = g.w * cellSize;
        int boardH = g.h * cellSize;

        startX = (getWidth() - boardW) / 2;
        startY = (getHeight() - boardH) / 2;

        Graphics2D g2 = (Graphics2D) gg;
        g2.setStroke(new BasicStroke(3));

        drawGrid(g2);
        drawPieces(g2);
        drawHighlightPath(g2);
        drawClues(g2);
    }

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

    private void drawPieces(Graphics2D g2) {
        for (int y = 0; y < g.h; y++) {
            for (int x = 0; x < g.w; x++) {
                int cx = startX + x * cellSize + cellSize / 2;
                int cy = startY + y * cellSize + cellSize / 2;
                TType t = g.board[y][x].t;

                if (g.board[y][x].clue) {
                    g2.setColor(new Color(220, 255, 220));
                    g2.fillRect(startX + x * cellSize + 2,
                            startY + y * cellSize + 2,
                            cellSize - 3, cellSize - 3);
                }

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

                if (g.board[y][x].start) {
                    g2.setColor(Color.BLUE);
                    g2.fillOval(cx - 10, cy - 10, 20, 20);
                    g2.setColor(Color.WHITE);
                    g2.drawString("A", cx - 4, cy + 5);
                }
                if (g.board[y][x].end) {
                    g2.setColor(Color.RED);
                    g2.fillOval(cx - 10, cy - 10, 20, 20);
                    g2.setColor(Color.WHITE);
                    g2.drawString("B", cx - 4, cy + 5);
                }
            }
        }
    }

    private void drawHighlightPath(Graphics2D g2) {
        if (highlightPath) {
            List<int[]> p = g.findPathFromCurrent();
            if (p != null) {
                g2.setColor(new Color(255, 200, 0, 140));
                for (int[] c : p) {
                    int x = c[0], y = c[1];
                    int sx = startX + x * cellSize;
                    int sy = startY + y * cellSize;
                    g2.fillRect(sx + 2, sy + 2, cellSize - 3, cellSize - 3);
                }
            }
        }
    }

    private void drawClues(Graphics2D g2) {
        int[] colCnt = new int[g.w], rowCnt = new int[g.h];
        for (int y = 0; y < g.h; y++) {
            for (int x = 0; x < g.w; x++) {
                if (g.sol[y][x] != TType.EMPTY) {
                    colCnt[x]++;
                    rowCnt[y]++;
                }
            }
        }
        g2.setColor(Color.BLACK);
        for (int x = 0; x < g.w; x++) {
            String s = "" + colCnt[x];
            int tx = startX + x * cellSize + cellSize / 2 - 6;
            g2.drawString(s, tx, startY - 12);
        }
        for (int y = 0; y < g.h; y++) {
            String s = "" + rowCnt[y];
            int ty = startY + y * cellSize + cellSize / 2 + 4;
            g2.drawString(s, startX - 25, ty);
        }
    }
}
