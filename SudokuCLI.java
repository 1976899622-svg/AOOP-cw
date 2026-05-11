import java.util.Observable;
import java.util.Observer;
import java.util.Scanner;
@SuppressWarnings("deprecation")
public final class SudokuCLI implements Observer {

    private final SudokuModel model;
    private final Scanner     in;

    public SudokuCLI(SudokuModel model) {
        this.model = model;
        this.in    = new Scanner(System.in);
        model.addObserver(this);
    }

    public static void main(String[] args) {
        new SudokuCLI(new Model()).run();
    }

    private void run() {
        printHelp();
        printBoard();
        while (true) {
            System.out.print("> ");
            if (!in.hasNextLine()) break;
            String line = in.nextLine().trim();
            if (line.isEmpty()) continue;
            if (!handle(line)) break;
        }
    }

    private boolean handle(String line) {
        String[] tk = line.split("\\s+");
        switch (tk[0].toLowerCase()) {
            case "set":   return doSet(tk);
            case "clear": return doClear(tk);
            case "undo":  model.undo();     printBoard(); return true;
            case "hint":  doHint();         return true;
            case "reset": model.reset();    printBoard(); return true;
            case "new":   model.newGame();  printBoard(); return true;
            case "show":  printBoard();     return true;
            case "help":  printHelp();      return true;
            case "quit":
            case "exit":  return false;
            default:
                System.out.println("Unknown command. Type 'help'.");
                return true;
        }
    }

    private boolean doSet(String[] tk) {
        if (tk.length != 4) { System.out.println("Usage: set <row> <col> <value>"); return true; }
        int r = parse(tk[1]) - 1, c = parse(tk[2]) - 1, v = parse(tk[3]);
        if (!inRange(r) || !inRange(c)) { System.out.println("Row/col must be 1..9"); return true; }
        if (v < 1 || v > 9)             { System.out.println("Value must be 1..9");  return true; }
        if (model.isFixed(r, c))        { System.out.println("That cell is pre-filled and cannot be changed."); return true; }
        model.setValue(r, c, v);
        if (model.isFeedbackEnabled() && model.isInvalid(r, c))
            System.out.println("Note: that move duplicates a value in the row, column or 3x3 box.");
        printBoard();
        return true;
    }

    private boolean doClear(String[] tk) {
        if (tk.length != 3) { System.out.println("Usage: clear <row> <col>"); return true; }
        int r = parse(tk[1]) - 1, c = parse(tk[2]) - 1;
        if (!inRange(r) || !inRange(c)) { System.out.println("Row/col must be 1..9"); return true; }
        if (model.isFixed(r, c))        { System.out.println("That cell is pre-filled."); return true; }
        model.clearCell(r, c);
        printBoard();
        return true;
    }

    private void doHint() {
        if (!model.isHintEnabled()) { System.out.println("Hints are disabled."); return; }
        if (model.isComplete())     { System.out.println("Puzzle is already complete."); return; }
        model.applyHint();
        printBoard();
    }

    private void printBoard() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n    1 2 3   4 5 6   7 8 9\n");
        sb.append("  +-------+-------+-------+\n");
        for (int r = 0; r < SudokuModel.SIZE; r++) {
            sb.append(r + 1).append(" |");
            for (int c = 0; c < SudokuModel.SIZE; c++) {
                sb.append(' ').append(symbolAt(r, c));
                if (c % 3 == 2) sb.append(" |");
            }
            sb.append('\n');
            if (r % 3 == 2) sb.append("  +-------+-------+-------+\n");
        }
        System.out.print(sb);
    }
    private String symbolAt(int r, int c) {
        int v = model.getValue(r, c);
        if (v == 0) return ".";
        boolean wrong = model.isFeedbackEnabled() && !model.isFixed(r, c) && model.isInvalid(r, c);
        return wrong ? "*" : String.valueOf(v);
    }

    private void printHelp() {
        System.out.println("Sudoku CLI commands:");
        System.out.println("  set   <row> <col> <value>   put a digit 1-9 in row,col (1-9)");
        System.out.println("  clear <row> <col>           clear an editable cell");
        System.out.println("  undo                        revert the last move");
        System.out.println("  hint                        reveal one correct cell");
        System.out.println("  reset                       restore the original puzzle");
        System.out.println("  new                         load a new puzzle");
        System.out.println("  show                        redraw the board");
        System.out.println("  help                        show this list");
        System.out.println("  quit                        exit");
    }


    @Override
    public void update(Observable o, Object arg) {
        if (model.isComplete())
            System.out.println("\n*** Puzzle solved. Well done! ***");
    }


    private static int     parse(String s) { try { return Integer.parseInt(s); } catch (Exception e) { return -1; } }
    private static boolean inRange(int v)  { return v >= 0 && v < SudokuModel.SIZE; }
}
