import java.util.Arrays;
final class Board {

    static final int SIZE = 9;
    static final int BOX  = 3;

    private final int[][] grid = new int[SIZE][SIZE];

    /* ---------- construction / mutation ---------- */

    Board() { /* all zeros */ }

    Board(int[][] source) { copyFrom(source); }

    void copyFrom(int[][] source) {
        for (int r = 0; r < SIZE; r++)
            System.arraycopy(source[r], 0, grid[r], 0, SIZE);
    }

    int[][] snapshot() {
        int[][] copy = new int[SIZE][SIZE];
        for (int r = 0; r < SIZE; r++)
            System.arraycopy(grid[r], 0, copy[r], 0, SIZE);
        return copy;
    }

    int  get(int r, int c)            { return grid[r][c]; }
    void set(int r, int c, int value) { grid[r][c] = value; }

    //True if value v at (r,c) duplicates the row, column or 3x3 box.
    boolean conflicts(int r, int c, int v) {
        if (v == 0) return false;
        for (int i = 0; i < SIZE; i++) {
            if (i != c && grid[r][i] == v) return true;
            if (i != r && grid[i][c] == v) return true;
        }
        int br = (r / BOX) * BOX, bc = (c / BOX) * BOX;
        for (int rr = br; rr < br + BOX; rr++)
            for (int cc = bc; cc < bc + BOX; cc++)
                if ((rr != r || cc != c) && grid[rr][cc] == v) return true;
        return false;
    }

    boolean isFull() {
        for (int[] row : grid)
            for (int v : row) if (v == 0) return false;
        return true;
    }
    // True if every cell is filled and no cell breaks a rule.
    boolean isComplete() {
        for (int r = 0; r < SIZE; r++)
            for (int c = 0; c < SIZE; c++) {
                int v = grid[r][c];
                if (v == 0)             return false;
                if (conflicts(r, c, v)) return false;
            }
        return true;
    }

    boolean solve() {
        for (int r = 0; r < SIZE; r++)
            for (int c = 0; c < SIZE; c++)
                if (grid[r][c] == 0) {
                    for (int v = 1; v <= SIZE; v++) {
                        if (!conflicts(r, c, v)) {
                            grid[r][c] = v;
                            if (solve()) return true;
                            grid[r][c] = 0;
                        }
                    }
                    return false;
                }
        return true;
    }

    @Override public String toString() { return Arrays.deepToString(grid); }
}
