package main.java.core.rect;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.awt.Color;

public class RectGrid {

    private final Map<RectCoord, Color> tiles;

    public RectGrid() {
        this.tiles = new HashMap<>();
    }

    /*
     * Returns the tile type at the given coordinate
     * Returns null if the coordinate is empty
     */
    public Color get(RectCoord coord) {
        return tiles.get(coord);
    }

    /*
     * Places a tile at the given coordinate.
     * If a tile already exists there, it is replaced.
     */
    public void set(RectCoord coord, Color color) {
        if (coord == null) { throw new IllegalArgumentException("Coordinate cannot be null."); }
        if (color == null) { throw new IllegalArgumentException("Color cannot be null."); }
        tiles.put(coord, color);
    }

    /*
     * Removes the tile at the given coordinate.
     * Return the removed tile, or null if the coordinate was empty.
     */
    public Color remove(RectCoord coord) {
        return tiles.remove(coord);
    }

    /*
     * Returns true if a tile exists at the given coordinate.
     */
    public boolean contains(RectCoord coord) {
        return tiles.containsKey(coord);
    }

    /*
     * Returns the six neighboring coordinates.
     */
    public List<RectCoord> neighborsOf(RectCoord coord) {
        return coord.getNeighbors();
    }

    /*
     * Returns all currently occupied coordinates.
     */
    public Set<RectCoord> getOccupiedCoordinates() {
        return tiles.keySet();
    }

    /*
     * Returns the number of occupied cells.
     */
    public int numOccupied() {
        return tiles.size();
    }

    /*
     * Removes every tile from the grid.
     */
    public void clear() {
        tiles.clear();
    }

    /*
     * Creates a deep copy of the grid structure.
     * HexCoord is immutable, and TileType is currently treated as a shared object, so the coordinates and tile references can safely be reused.
     */
    public RectGrid copy() {
        RectGrid copy = new RectGrid();
        copy.tiles.putAll(this.tiles);
        return copy;
    }

    /*
     * Returns a map containing the current grid contents.
     */
    public Map<RectCoord, Color> getTiles() {
        return tiles;
    }
}
