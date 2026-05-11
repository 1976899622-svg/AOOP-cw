import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Random;

/**
 * The Sudoku Model. Extends {@link Observable} as required by the rubric,
 * implements {@link SudokuModel} so that the View, Controller and JUnit
 * tests are all decoupled from the concrete class.
 *
 * Class invariants (informal):
 *   I1.  board != null && initial != null && solution != null
 *   I2.  every cell value v satisfies 0 <= v <= 9
 *   I3.  every fixed (initial != 0) cell holds its initial value forever
 *   I4.  solution is a complete legal solution of the initial puzzle
 */
@SuppressWarnings("deprecation")          // Observable is required by the brief
public class Model extends Observable implements SudokuModel {

    /* ---------- state ---------- */

    private final Board board    = new Board();
    private int[][]     initial  = new int[SIZE][SIZE];   // pre-filled puzzle
    private int[][]     solution = new int[SIZE][SIZE];   // solved puzzle (for hints)

    private boolean feedback     = true;
    private boolean hintEnabled  = true;
    private boolean randomPick   = true;

    private final List<int[][]> puzzles = new ArrayList<>();
    private final Random        rng     = new Random();

    /** Single-level undo: the value of one cell before the last edit. */
    private int undoRow = -1, undoCol = -1, undoValue = 0;

    /* ---------- construction ---------- */

    public Model() {
        loadPuzzles();
        newGame();
        assert invariant();
    }

    private boolean invariant() {
        if (board == null || initial == null || solution == null) return false;
        for (int r = 0; r < SIZE; r++)
            for (int c = 0; c < SIZE; c++) {
                int v = board.get(r, c);
                if (v < 0 || v > SIZE) return false;
                if (initial[r][c] != 0 && board.get(r, c) != initial[r][c]) return false;
            }
        return true;
    }

    /* ---------- file loading (FR3 puzzle source, NFR1 file IO in Model) ---------- */

    private void loadPuzzles() {
        try (InputStream in = Model.class.getResourceAsStream("/puzzles.txt")) {
            if (in != null) parsePuzzles(in);
        } catch (IOException ignored) { /* fall through */ }
        if (puzzles.isEmpty()) puzzles.add(defaultPuzzle());
    }

    private void parsePuzzles(InputStream in) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        List<String> allLines = new ArrayList<>();
        String line;

        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (!line.isEmpty()) {
                allLines.add(line);
            }
        }

        if (allLines.isEmpty()) return;

        String firstLine = allLines.get(0);

        if (firstLine.length() == SIZE * SIZE) {
            for (String puzzleLine : allLines) {
                if (puzzleLine.length() >= SIZE * SIZE) {
                    int[][] puzzle = new int[SIZE][SIZE];
                    for (int r = 0; r < SIZE; r++) {
                        for (int c = 0; c < SIZE; c++) {
                            char ch = puzzleLine.charAt(r * SIZE + c);
                            puzzle[r][c] = (ch >= '1' && ch <= '9') ? ch - '0' : 0;
                        }
                    }
                    puzzles.add(puzzle);
                }
            }
        }
        else {
            List<String> rows = new ArrayList<>();
            for (String row : allLines) {
                if (row.length() >= SIZE) {
                    rows.add(row.substring(0, SIZE));
                    if (rows.size() == SIZE) {
                        addPuzzleFrom(rows);
                        rows.clear();
                    }
                }
            }
        }
    }

    private void addPuzzleFrom(List<String> rows) {
        if (rows.size() != SIZE) return;
        int[][] puzzle = new int[SIZE][SIZE];
        for (int r = 0; r < SIZE; r++)
            for (int c = 0; c < SIZE; c++) {
                char ch = rows.get(r).charAt(c);
                puzzle[r][c] = (ch >= '1' && ch <= '9') ? ch - '0' : 0;
            }
        puzzles.add(puzzle);
    }

    private static int[][] defaultPuzzle() {
        return new int[][] {
            {5,3,0, 0,7,0, 0,0,0},
            {6,0,0, 1,9,5, 0,0,0},
            {0,9,8, 0,0,0, 0,6,0},
            {8,0,0, 0,6,0, 0,0,3},
            {4,0,0, 8,0,3, 0,0,1},
            {7,0,0, 0,2,0, 0,0,6},
            {0,6,0, 0,0,0, 2,8,0},
            {0,0,0, 4,1,9, 0,0,5},
            {0,0,0, 0,8,0, 0,7,9}
        };
    }

    /* ---------- queries ---------- */

    @Override public int     getValue(int r, int c)  { return board.get(r, c); }
    @Override public boolean isFixed (int r, int c)  { return initial[r][c] != 0; }
    @Override public boolean canUndo()               { return undoRow >= 0; }

    @Override public boolean isInvalid(int r, int c) {
        int v = board.get(r, c);
        return v != 0 && board.conflicts(r, c, v);
    }

    @Override public boolean isComplete() { return board.isComplete(); }

    @Override public boolean isFeedbackEnabled() { return feedback;    }
    @Override public boolean isHintEnabled()     { return hintEnabled; }
    @Override public boolean isRandomPuzzle()    { return randomPick;  }

    /* ---------- flag setters ---------- */

    @Override public void setFeedbackEnabled(boolean v) { feedback    = v; publish(); }
    @Override public void setHintEnabled(boolean v)     { hintEnabled = v; publish(); }
    @Override public void setRandomPuzzle(boolean v)    { randomPick  = v; publish(); }

    /* ---------- commands ---------- */
    @Override public void setValue(int row, int col, int value) {
        assert row >= 0 && row < SIZE && col >= 0 && col < SIZE
                : "row/col out of range";
        assert value >= 1 && value <= SIZE : "value must be 1..9";
        assert !isFixed(row, col)          : "cannot edit a pre-filled cell";

        recordUndo(row, col);
        board.set(row, col, value);

        assert getValue(row, col) == value : "value not stored";
        assert invariant();
        publish();
    }

    @Override public void clearCell(int row, int col) {
        assert row >= 0 && row < SIZE && col >= 0 && col < SIZE;
        assert !isFixed(row, col) : "cannot clear a pre-filled cell";

        recordUndo(row, col);
        board.set(row, col, 0);

        assert getValue(row, col) == 0;
        assert invariant();
        publish();
    }
    @Override public void undo() {
        assert canUndo() : "nothing to undo";
        board.set(undoRow, undoCol, undoValue);
        undoRow = undoCol = -1; undoValue = 0;
        assert !canUndo();
        assert invariant();
        publish();
    }
    @Override public void applyHint() {
        if (!hintEnabled || isComplete()) return;
        for (int r = 0; r < SIZE; r++)
            for (int c = 0; c < SIZE; c++)
                if (board.get(r, c) == 0) {
                    recordUndo(r, c);
                    board.set(r, c, solution[r][c]);
                    assert getValue(r, c) == solution[r][c];
                    assert invariant();
                    publish();
                    return;
                }
    }
    @Override public void reset() {
        board.copyFrom(initial);
        undoRow = undoCol = -1; undoValue = 0;
        assert invariant();
        publish();
    }
    @Override public void newGame() {
        int idx = randomPick ? rng.nextInt(puzzles.size()) : 0;
        int[][] picked = puzzles.get(idx);

        initial = deepCopy(picked);
        board.copyFrom(initial);

        Board solver = new Board(initial);
        solver.solve();
        solution = solver.snapshot();

        undoRow = undoCol = -1; undoValue = 0;
        assert invariant();
        publish();
    }

    /* ---------- helpers ---------- */

    private void recordUndo(int row, int col) {
        undoRow = row;
        undoCol = col;
        undoValue = board.get(row, col);
    }

    private void publish() { setChanged(); notifyObservers(); }

    private static int[][] deepCopy(int[][] src) {
        int[][] copy = new int[src.length][];
        for (int r = 0; r < src.length; r++) copy[r] = src[r].clone();
        return copy;
    }

    /* ---------- visible only for testing ---------- */
    void loadFixedPuzzle(int[][] puzzle) {
        initial = deepCopy(puzzle);
        board.copyFrom(initial);

        Board solver = new Board(initial);
        solver.solve();
        solution = solver.snapshot();

        undoRow = undoCol = -1; undoValue = 0;
        assert invariant();
        publish();
    }
    int solutionAt(int r, int c) { return solution[r][c]; }
}
