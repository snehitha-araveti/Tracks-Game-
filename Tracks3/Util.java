package Tracks;

import java.util.HashSet;
import java.util.Set;

/**
 * Utility functions for track piece and direction operations.
 */
public class Util {

    /**
     * Cycle order for track types when user clicks a cell.
     */
    static TType[] cycle = {
        TType.EMPTY,
        TType.HR,
        TType.VY,
        TType.NE,
        TType.NW,
        TType.SE,
        TType.SW
    };

    /**
     * Returns the next track type in the cycle. Wraps around to EMPTY at the
     * end.
     */
    public static TType nextType(TType cur) {
        for (int i = 0; i < cycle.length; i++) {
            if (cycle[i] == cur) {
                return cycle[(i + 1) % cycle.length];
            }
        }
        return TType.EMPTY;
    }

    /**
     * Maps a tile type to the two directions it connects. EMPTY returns an
     * empty set.
     */
    public static Set<Dir> dirsOf(TType t) {
        Set<Dir> s = new HashSet<>();
        switch (t) {
            case HR:
                s.add(Dir.L);
                s.add(Dir.R);
                break;
            case VY:
                s.add(Dir.U);
                s.add(Dir.D);
                break;
            case NE:
                s.add(Dir.U);
                s.add(Dir.R);
                break;
            case NW:
                s.add(Dir.U);
                s.add(Dir.L);
                break;
            case SE:
                s.add(Dir.D);
                s.add(Dir.R);
                break;
            case SW:
                s.add(Dir.D);
                s.add(Dir.L);
                break;
            default:
                break;
        }
        return s;
    }

    /**
     * Returns the opposite direction (U↔D, L↔R).
     */
    public static Dir opposite(Dir d) {
        switch (d) {
            case U:
                return Dir.D;
            case D:
                return Dir.U;
            case L:
                return Dir.R;
            default:
                return Dir.L;
        }
    }

    /**
     * Converts two directions back to the corresponding tile type. Returns
     * EMPTY if input is invalid.
     */
    public static TType typeFromDirs(Set<Dir> s) {
        if (s.size() != 2) {
            return TType.EMPTY;
        }
        if (s.contains(Dir.L) && s.contains(Dir.R)) {
            return TType.HR;
        }
        if (s.contains(Dir.U) && s.contains(Dir.D)) {
            return TType.VY;
        }
        if (s.contains(Dir.U) && s.contains(Dir.R)) {
            return TType.NE;
        }
        if (s.contains(Dir.U) && s.contains(Dir.L)) {
            return TType.NW;
        }
        if (s.contains(Dir.D) && s.contains(Dir.R)) {
            return TType.SE;
        }
        if (s.contains(Dir.D) && s.contains(Dir.L)) {
            return TType.SW;
        }
        return TType.EMPTY;
    }
}
