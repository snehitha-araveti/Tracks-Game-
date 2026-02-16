package Tracks;

import javax.swing.*;
import java.awt.*;

public class TracksGame extends JFrame {
    private Game gm;
    private BoardPanel bp;
    private JPanel topBar;
    private JButton btnNew, btnRestart, btnUndo, btnSolve, btnCheck;
    private JLabel lblMsg;

    private int setW = 8, setH = 8, setDiff = 40;

    public TracksGame() {
        super("Tracks");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 720);
        setLocationRelativeTo(null);
        initUI();
        newGameDialog();
        setVisible(true);
    }

    private void initUI() {
        setLayout(new BorderLayout());
        topBar = new JPanel();

        btnNew = new JButton("New Game");
        btnRestart = new JButton("Restart");
        btnUndo = new JButton("Undo");
        btnSolve = new JButton("Show Solution");
        btnCheck = new JButton("Check");

        btnNew.setBackground(new Color(76, 175, 80));
        btnRestart.setBackground(new Color(255, 193, 7));
        btnUndo.setBackground(new Color(33, 150, 243));
        btnSolve.setBackground(new Color(244, 67, 54));
        btnCheck.setBackground(new Color(156, 39, 176));

        btnNew.setForeground(Color.WHITE);
        btnRestart.setForeground(Color.BLACK);
        btnUndo.setForeground(Color.WHITE);
        btnSolve.setForeground(Color.WHITE);
        btnCheck.setForeground(Color.WHITE);

        topBar.add(btnNew);
        topBar.add(btnRestart);
        topBar.add(btnUndo);
        topBar.add(btnSolve);
        topBar.add(btnCheck);

        lblMsg = new JLabel("Welcome to Tracks ");
        topBar.add(lblMsg);
        add(topBar, BorderLayout.NORTH);

        bp = new BoardPanel(this);
        add(new JScrollPane(bp), BorderLayout.CENTER);

        setupListeners();
    }

    private void setupListeners() {
        btnNew.addActionListener(e -> newGameDialog());
        btnRestart.addActionListener(e -> {
            if (gm != null) {
                gm.restart();
                bp.highlightPath = false;
                bp.repaint();
                lblMsg.setText("Restarted");
            }
        });
        btnUndo.addActionListener(e -> {
            if (gm != null) {
                gm.undo();
                bp.highlightPath = false;
                bp.repaint();
                lblMsg.setText("Undo");
            }
        });
        btnSolve.addActionListener(e -> {
            if (gm != null) {
                gm.revealSolution();
                gm.revealedSolution = true;
                bp.highlightPath = true;
                bp.repaint();
                lblMsg.setText("Solution shown");
            }
        });
        btnCheck.addActionListener(e -> {
            if (gm != null) {
                boolean ok = gm.checkSolved();
                if (ok) {
                    bp.highlightPath = true;
                    bp.repaint();
                    lblMsg.setText("Solved!");
                } else {
                    lblMsg.setText("Not solved yet.");
                }
            }
        });
    }

    private void newGameDialog() {
        JPanel p = new JPanel(new GridLayout(3, 2, 6, 6));
        JTextField tfW = new JTextField("" + setW);
        JTextField tfH = new JTextField("" + setH);
        String[] diffs = {"Easy (50%)", "Medium (35%)", "Hard (20%)"};
        JComboBox<String> cbDiff = new JComboBox<>(diffs);
        cbDiff.setSelectedIndex(setDiff >= 40 ? 0 : setDiff >= 30 ? 1 : 2);
        p.add(new JLabel("Width:"));
        p.add(tfW);
        p.add(new JLabel("Height:"));
        p.add(tfH);
        p.add(new JLabel("Difficulty:"));
        p.add(cbDiff);

        int res = JOptionPane.showConfirmDialog(this, p, "New Game Settings", JOptionPane.OK_CANCEL_OPTION);
        if (res != JOptionPane.OK_OPTION) {
            return;
        }
        try {
            int nw = Integer.parseInt(tfW.getText().trim());
            int nh = Integer.parseInt(tfH.getText().trim());
            if (nw < 4 || nh < 4) {
                throw new NumberFormatException();
            }
            setW = nw;
            setH = nh;
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Invalid size; using defaults.");
        }
        int di = cbDiff.getSelectedIndex();
        setDiff = di == 0 ? 50 : di == 1 ? 35 : 20;

        Game g;
        int tries = 0;
        do {
            g = new Game(setW, setH);
            tries++;
        } while (!g.genPathAndSolution(setDiff) && tries < 20);

        if (tries >= 20) {
            JOptionPane.showMessageDialog(this, "Couldn't generate puzzle — try different size.");
            return;
        }

        this.gm = g;
        bp.setGame(gm);
        bp.highlightPath = false;
        bp.repaint();
        lblMsg.setText("New game " + setW + "x" + setH + " created.");
    }

    public void setMessage(String msg) {
        lblMsg.setText(msg);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new TracksGame());
    }
}
