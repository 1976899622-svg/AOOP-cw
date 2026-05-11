import javax.swing.SwingUtilities;

public final class SudokuGUI {

    private SudokuGUI() { /* no instances */ }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            SudokuModel model      = new Model();
            Controller  controller = new Controller(model);
            new View(model, controller);
        });
    }
}
