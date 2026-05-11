public class Controller {

    private final SudokuModel model;
    private View view;

    public Controller(SudokuModel model) { this.model = model; }

    public void setView(View view) {
        this.view = view;
        refreshButtons();
    }

    public void cellTyped(int row, int col, int value) {
        if (model.isFixed(row, col))     return;
        if (value < 1 || value > 9)      return;
        model.setValue(row, col, value);
        refreshButtons();
    }

    public void erase(int row, int col) {
        if (row < 0 || col < 0)          return;
        if (model.isFixed(row, col))     return;
        if (model.getValue(row, col) == 0) return;
        model.clearCell(row, col);
        refreshButtons();
    }

    public void undo() {
        if (!model.canUndo()) return;
        model.undo();
        refreshButtons();
    }

    public void hint() {
        if (!model.isHintEnabled() || model.isComplete()) return;
        model.applyHint();
        refreshButtons();
    }

    public void reset()    { model.reset();    refreshButtons(); }
    public void newGame()  { model.newGame();  refreshButtons(); }

    public void setFeedbackEnabled(boolean v) { model.setFeedbackEnabled(v); }
    public void setHintEnabled    (boolean v) { model.setHintEnabled(v);     refreshButtons(); }
    public void setRandomPuzzle   (boolean v) { model.setRandomPuzzle(v);    }

    private void refreshButtons() {
        if (view == null) return;
        view.setUndoEnabled(model.canUndo());
        view.setHintEnabled(model.isHintEnabled() && !model.isComplete());
    }
}
