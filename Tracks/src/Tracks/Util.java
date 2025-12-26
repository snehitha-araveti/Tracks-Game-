package Tracks;

import java.util.HashSet;
import java.util.Set;

public class Util {
    static TType[] cycle = {TType.EMPTY, TType.HR, TType.VY, TType.NE, TType.NW, TType.SE, TType.SW};
    
    // to rotate a tile to get correct connection 
    //cycle EMPTY → HR → VY → NE → NW → SE → SW → EMPTY
    // O(1) - Array lookup
    
    public static TType nextType(TType cur) {
        for (int i = 0; i < cycle.length; i++) {
            if (cycle[i] == cur) {
                return cycle[(i + 1) % cycle.length];
            }
        }
        return TType.EMPTY;
    }

    // Get connections of a track piece
    // for example for horitonal = left + right enum..
    // why hashset fast lookup,prevent duplicate direction
    
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

    // For validating connections between adjacent cells
    
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

    //During puzzle generation to determine correct piece type
    
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


