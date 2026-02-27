package Tracks;

/**

 *  *Represents the four cardinal directions on the game grid.

 *  *Used throughout the game to:

 *  *Define connections between adjacent cells Calculate neighbor positions Map
 * tile types to their connecting directions
*
 */
public enum Dir {
    /**
     * Up direction (negative Y axis)
     */
    U,
    /**
     * Down direction (positive Y axis)
     */
    D,
    /**
     * Left direction (negative X axis)
     */
    L,
    /**
     * Right direction (positive X axis)
     */
    R
}
