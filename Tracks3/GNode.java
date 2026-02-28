package Tracks;

import java.util.ArrayList;
import java.util.List;

/**
 * Graph node representing a cell on the game board.
 * Used for building the connectivity graph and pathfinding.
 */
public class GNode {

    /** X coordinate on the board */
    public int x;

    /** Y coordinate on the board */
    public int y;

    /** Adjacent nodes connected by tracks */
    public List<GNode> nbrs = new ArrayList<>();

    /**
     * Creates a new graph node.
     *
     *  x  X coordinate
     *  y  Y coordinate
     */
    public GNode(int x, int y) {
        this.x = x;
        this.y = y;
    }
}
