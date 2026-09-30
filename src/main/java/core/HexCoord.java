package main.java.core;

import java.util.List;

public class HexCoord {

    // use axial coordinates
    public final int q;
    public final int r;

    public HexCoord() {
        this.q = 0; this.r = 0;
    }

    public HexCoord(int q, int r) {
        this.q = q; this.r = r;
    }

    @Override 
    public int hashCode() {
        int code = 17; 
        code = 31 * code + q;
        code = 31 * code + r;
        return code;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) { return true; }
        if (obj == null || getClass() != obj.getClass()) { return false; }
        HexCoord c = (HexCoord) obj;
        return this.q == c.q && this.r == c.r;
    }

    @Override 
    public String toString() {
        return "(" + q + ", " + r + ")";
    }

    /*
     * Returns a List of the six adjacent HexCoords
     */
    public List<HexCoord> getNeighbors() {
        return List.of(
            new HexCoord(q + 1, r),
            new HexCoord(q , r + 1), 
            new HexCoord(q - 1, r + 1), 
            new HexCoord(q - 1, r),
            new HexCoord(q, r - 1),
            new HexCoord(q + 1, r - 1)
        );
    }

    /* 
     * Calculates and returns the linear distance between two HexCoords
     */
    public static double linearDistance(HexCoord c1, HexCoord c2) {
        // find distance along axes
        int dq = c2.q - c1.q;
        int dr = c2.r - c1.r;
        // convert to cartesian
        double dx = Math.sqrt(3) * (dq + dr / 2.0);
        double dy = 3.0 / 2.0 * dr;
        // calculate distance
        return Math.sqrt(dx * dx + dy * dy);
    }

    /* 
     * Calculates and returns the length of the shortest path between two HexCoords
     */
    public static int pathDistance(HexCoord a, HexCoord b) {
        // find distance along axes
        int dq = b.q - a.q;
        int dr = b.r - a.r;
        // find path
        return Math.max(
            Math.max(
                Math.abs(dq),
                Math.abs(dr)
            ),
            Math.abs(dq + dr)
        );
    }
    
}
