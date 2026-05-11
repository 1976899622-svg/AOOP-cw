import java.util.Observer;
public interface SudokuModel {

    int SIZE = 9;
    int BOX  = 3;

    int  getValue(int row, int col);

    boolean isFixed(int row, int col);

    boolean isInvalid(int row, int col);

    boolean isComplete();

    boolean canUndo();

    /* ---------- flags (FR3) ---------- */

    boolean isFeedbackEnabled();
    boolean isHintEnabled();
    boolean isRandomPuzzle();
    void    setFeedbackEnabled(boolean enabled);
    void    setHintEnabled(boolean enabled);
    void    setRandomPuzzle(boolean random);

    /* ---------- commands ---------- */

    void setValue(int row, int col, int value);

    //Clear an editable cell (FR5 Erase)
    void clearCell(int row, int col);

    //Revert the most recent user action (single-level undo, FR5).
    void undo();

    //Apply one correct hint, if hints are enabled (FR5).
    void applyHint();

    //Restore the puzzle to its initial state (FR5).
    void reset();

    //Load a new puzzle from puzzles.txt (FR5).
    void newGame();

    void addObserver(Observer o);
}
