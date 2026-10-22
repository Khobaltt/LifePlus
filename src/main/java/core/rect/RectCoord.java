package main.java.core.rect;

import java.util.List;

public class RectCoord {

    public static final int MAX_X_COORD = 1023;
    public static final int MIN_X_COORD = -1024;
    public static final int MAX_Y_COORD = 1023;
    public static final int MIN_Y_COORD = -1024;

    // use axial coordinates
    public final int x;
    public final int y;

    public RectCoord() {
        this.x = 0; 
        this.y = 0;
    }

    public RectCoord(int x, int y) {
        this.x = x; 
        this.y = y;
    }

    @Override 
    public int hashCode() {
        int code = 17; 
        code = 31 * code + x;
        code = 31 * code + y;
        return code;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) { return true; }
        if (obj == null || getClass() != obj.getClass()) { return false; }
        RectCoord c = (RectCoord) obj;
        return this.x == c.x && this.y == c.y;
    }

    @Override 
    public String toString() {
        return "(" + x + ", " + y + ")";
    }

    /*
     * Returns a List of the eight adjacent RectCoords (or as many as are in bounds)
     */
    public List<RectCoord> getNeighbors() {
        List<RectCoord> neighbors = List.of(
            new RectCoord(x + 1, y),
            new RectCoord(x + 1 , y + 1), 
            new RectCoord(x, y + 1), 
            new RectCoord(x - 1, y + 1),
            new RectCoord(x - 1, y),
            new RectCoord(x - 1, y - 1),
            new RectCoord(x, y - 1),
            new RectCoord(x + 1, y - 1)
        );
        for (RectCoord c : neighbors) {
            if (!c.isInBounds()) { neighbors.remove(c); }
        }
        return neighbors;
    }

    /* 
     * Checks if a RectCoord is within the grid bounds
     */
    public boolean isInBounds() {
        return this.x <= MAX_X_COORD &&
               this.x >= MIN_X_COORD &&
               this.y <= MAX_Y_COORD &&
               this.y >= MIN_Y_COORD;
    }

    /* 
     * Calculates and returns the linear distance between two HexCoords
     */
    public static double linearDistance(RectCoord a, RectCoord b) {
        // find distance along axes
        int dx = a.x - b.x;
        int dy = a.y - b.y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    /* 
     * Calculates and returns the length of the shortest path between two HexCoords
     */
    public static int pathDistance(RectCoord a, RectCoord b) {
        // find distance along axes
        int dx = b.x - a.x;
        int dy = b.y - a.y;
        // find path
        return Math.abs(dx) + Math.abs(dy);
    }
}
