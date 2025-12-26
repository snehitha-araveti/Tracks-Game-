package Tracks;

public class Move {
	// keep track of moves in the cell
    public int x, y;
    public TType prev;
    public boolean prevClue;

    public Move(int x, int y, TType prev, boolean prevClue) {
        this.x = x;
        this.y = y;
        this.prev = prev;
        this.prevClue = prevClue;
    }
}
