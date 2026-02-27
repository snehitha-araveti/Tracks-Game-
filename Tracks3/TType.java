package Tracks;

/**
 * Represents the different types of track pieces that can be placed on the
 * board.
 *
 * Each enum constant corresponds to a specific track layout: - EMPTY: No track
 * (empty cell) - HR: Horizontal track (left-right) - VY: Vertical track
 * (up-down) - NE, NW, SE, SW: Curved tracks connecting two directions
 *
 */
public enum TType {

    /**
     * Empty cell with no track piece
     */
    EMPTY,
    /**
     * Horizontal track connecting Left and Right
     */
    HR,
    /**
     * Vertical track connecting Up and Down
     */
    VY,
    /**
     * Curved track connecting North and East
     */
    NE,
    /**
     * Curved track connecting North and West
     */
    NW,
    /**
     * Curved track connecting South and East
     */
    SE,
    /**
     * Curved track connecting South and West
     */
    SW
}
