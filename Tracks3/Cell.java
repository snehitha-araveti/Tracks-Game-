package Tracks;

public class Cell {
	/** Track type in this cell */
	public TType t = TType.EMPTY;

	/** Whether this cell is a clue (pre-filled) */
	public boolean clue = false;

	/** Whether this cell is the start point */
	public boolean start = false;

	/** Whether this cell is the end point */
	public boolean end = false;

	/**
	 * Creates a copy of this cell.
	 *
	 * Copy with same values
	 */
	public Cell copy() {
	    Cell c = new Cell();
	    c.t = this.t;
	    c.clue = this.clue;
	    c.start = this.start;
	    c.end = this.end;
	    return c;
	}
	}
