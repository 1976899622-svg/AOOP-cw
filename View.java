import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Observable;
import java.util.Observer;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.LineBorder;
@SuppressWarnings("deprecation")
public class View implements Observer {

    private static final int    SIZE       = SudokuModel.SIZE;
    private static final int    BOX        = SudokuModel.BOX;
    private static final Color  FIXED_BG   = new Color(0xE8EEF7);
    private static final Color  EDIT_BG    = Color.WHITE;
    private static final Color  SELECT_BG  = new Color(0xC8E1FF);
    private static final Color  ERROR_FG   = new Color(0xC8102E);
    private static final Font   CELL_FONT  = new Font(Font.SANS_SERIF, Font.PLAIN, 22);
    private static final Font   FIXED_FONT = new Font(Font.SANS_SERIF, Font.BOLD,  22);

    private final SudokuModel model;
    private final Controller  controller;

    private final JFrame        frame   = new JFrame("Sudoku");
    private final JTextField[][] cells  = new JTextField[SIZE][SIZE];

    private final JButton eraseBtn   = new JButton("Erase");
    private final JButton undoBtn    = new JButton("Undo");
    private final JButton hintBtn    = new JButton("Hint");
    private final JButton resetBtn   = new JButton("Reset");
    private final JButton newGameBtn = new JButton("New Game");

    private final JCheckBox feedbackBox = new JCheckBox("Show invalid moves",  true);
    private final JCheckBox hintBox     = new JCheckBox("Hints enabled",       true);
    private final JCheckBox randomBox   = new JCheckBox("Random puzzle",       true);

    private int selectedRow = 0;
    private int selectedCol = 0;
    private boolean alreadyAnnouncedWin = false;

    public View(SudokuModel model, Controller controller) {
        this.model      = model;
        this.controller = controller;

        buildGrid();
        buildControls();

        model.addObserver(this);
        controller.setView(this);

        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

        update(null, null);          // initial paint
    }

    /* ---------- layout ---------- */

    private void buildGrid() {
        JPanel boardPanel = new JPanel(new GridLayout(BOX, BOX, 4, 4));
        boardPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel[][] boxes = new JPanel[BOX][BOX];
        for (int br = 0; br < BOX; br++)
            for (int bc = 0; bc < BOX; bc++) {
                boxes[br][bc] = new JPanel(new GridLayout(BOX, BOX, 1, 1));
                boxes[br][bc].setBorder(new LineBorder(Color.DARK_GRAY, 1));
                boardPanel.add(boxes[br][bc]);
            }

        for (int r = 0; r < SIZE; r++)
            for (int c = 0; c < SIZE; c++) {
                JTextField cell = new JTextField();
                cell.setHorizontalAlignment(SwingConstants.CENTER);
                cell.setFont(CELL_FONT);
                cell.setPreferredSize(new Dimension(48, 48));
                cell.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
                cell.setEditable(false);
                final int row = r, col = c;
                cell.addMouseListener(new java.awt.event.MouseAdapter() {
                    @Override public void mousePressed(java.awt.event.MouseEvent e) {
                        selectedRow = row;
                        selectedCol = col;
                        update(null, null);
                    }
                });
                cell.addKeyListener(new KeyAdapter() {
                    @Override public void keyTyped(KeyEvent e) {
                        char ch = e.getKeyChar();
                        if (ch >= '1' && ch <= '9')
                            controller.cellTyped(row, col, ch - '0');
                        else if (ch == KeyEvent.VK_BACK_SPACE || ch == KeyEvent.VK_DELETE || ch == '0')
                            controller.erase(row, col);
                    }
                });
                cells[r][c] = cell;
                boxes[r / BOX][c / BOX].add(cell);
            }

        frame.add(boardPanel, BorderLayout.CENTER);
    }

    private void buildControls() {
        JPanel right = new JPanel();
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        right.setBorder(BorderFactory.createEmptyBorder(8, 4, 8, 8));

        JPanel keypad = new JPanel(new GridLayout(3, 3, 4, 4));
        for (int n = 1; n <= 9; n++) {
            JButton b = new JButton(String.valueOf(n));
            b.setFont(FIXED_FONT);
            final int v = n;
            b.addActionListener(e -> controller.cellTyped(selectedRow, selectedCol, v));
            keypad.add(b);
        }
        keypad.setMaximumSize(new Dimension(200, 160));
        right.add(keypad);
        right.add(Box.createVerticalStrut(8));

        eraseBtn  .addActionListener(e -> controller.erase(selectedRow, selectedCol));
        undoBtn   .addActionListener(e -> controller.undo());
        hintBtn   .addActionListener(e -> controller.hint());
        resetBtn  .addActionListener(e -> controller.reset());
        newGameBtn.addActionListener(e -> { alreadyAnnouncedWin = false; controller.newGame(); });

        for (JButton b : new JButton[] {eraseBtn, undoBtn, hintBtn, resetBtn, newGameBtn}) {
            b.setAlignmentX(JButton.CENTER_ALIGNMENT);
            b.setMaximumSize(new Dimension(200, 32));
            right.add(b);
            right.add(Box.createVerticalStrut(4));
        }

        right.add(Box.createVerticalStrut(8));

        feedbackBox.addActionListener(e -> controller.setFeedbackEnabled(feedbackBox.isSelected()));
        hintBox    .addActionListener(e -> controller.setHintEnabled    (hintBox.isSelected()));
        randomBox  .addActionListener(e -> controller.setRandomPuzzle   (randomBox.isSelected()));

        for (JCheckBox cb : new JCheckBox[] {feedbackBox, hintBox, randomBox}) {
            cb.setAlignmentX(JCheckBox.LEFT_ALIGNMENT);
            right.add(cb);
        }

        frame.add(right, BorderLayout.EAST);
    }

    /* ---------- callbacks the Controller can use to enable/disable buttons ---------- */

    void setUndoEnabled(boolean enabled) { undoBtn.setEnabled(enabled); }
    void setHintEnabled(boolean enabled) { hintBtn.setEnabled(enabled); }

    /* ---------- Observer: pull fresh state from the model ---------- */

    @Override
    public void update(Observable o, Object arg) {
        for (int r = 0; r < SIZE; r++)
            for (int c = 0; c < SIZE; c++)
                paintCell(r, c);

        if (model.isComplete() && !alreadyAnnouncedWin) {
            alreadyAnnouncedWin = true;
            JOptionPane.showMessageDialog(frame,
                    "Puzzle solved. Well done!",
                    "Sudoku", JOptionPane.INFORMATION_MESSAGE);
        }
        if (!model.isComplete()) alreadyAnnouncedWin = false;
    }

    private void paintCell(int r, int c) {
        JTextField cell = cells[r][c];
        int v = model.getValue(r, c);
        cell.setText(v == 0 ? "" : String.valueOf(v));

        if (model.isFixed(r, c)) {
            cell.setBackground(FIXED_BG);
            cell.setFont(FIXED_FONT);
            cell.setForeground(Color.BLACK);
        } else {
            cell.setBackground(r == selectedRow && c == selectedCol ? SELECT_BG : EDIT_BG);
            cell.setFont(CELL_FONT);
            boolean wrong = model.isFeedbackEnabled() && model.isInvalid(r, c);
            cell.setForeground(wrong ? ERROR_FG : new Color(0x1F4E79));
        }
    }
}
