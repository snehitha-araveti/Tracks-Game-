package Tracks;

import java.util.ArrayList;

public class GNode {
    public int x, y;  //grid coordinates
    public ArrayList<GNode> nbrs = new ArrayList<>(); 
    //keep track adjacent connected nodes

    public GNode(int x, int y) {
        this.x = x;
        this.y = y;
    }
}
