package Tracks;

/**
 * Represents a single move in the game.
 * Stores the cell position and the previous state before the move.
 *
 */
public class Move {

    /** X coordinate of the cell */
    public int x;

    /** Y coordinate of the cell */
    public int y;

    /** Previous track type at this cell before the move */
    public TType prev;

    /** Whether this cell was a clue before the move */
    public boolean prevClue;

    /**
     * Creates a new move record.
     *
     * x         X coordinate of the cell
     * y         Y coordinate of the cell
     * prev      Previous track type
     * prevClue  Whether it was a clue before
     */
    public Move(int x, int y, TType prev, boolean prevClue) {
        this.x = x; this.y = y;
        this.prev = prev; this.prevClue = prevClue;
    }
}
