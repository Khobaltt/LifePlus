package main.java.core;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class HexGrid {

    private final Map<HexCoord, TileType> tiles;

    public HexGrid() {
        this.tiles = new HashMap<>();
    }

    /*
     * Returns the tile type at the given coordinate
     * Returns null if the coordinate is empty
     */
    public TileType get(HexCoord coord) {
        return tiles.get(coord);
    }

    /*
     * Places a tile at the given coordinate.
     * If a tile already exists there, it is replaced.
     */
    public void set(HexCoord coord, TileType tile) {
        if (coord == null) { throw new IllegalArgumentException("Coordinate cannot be null."); }
        if (tile == null) { throw new IllegalArgumentException("Tile cannot be null."); }
        tiles.put(coord, tile);
    }

    /*
     * Removes the tile at the given coordinate.
     * Return the removed tile, or null if the coordinate was empty.
     */
    public TileType remove(HexCoord coord) {
        return tiles.remove(coord);
    }

    /*
     * Returns true if a tile exists at the given coordinate.
     */
    public boolean contains(HexCoord coord) {
        return tiles.containsKey(coord);
    }

    /*
     * Returns the six neighboring coordinates.
     */
    public List<HexCoord> neighborsOf(HexCoord coord) {
        return coord.getNeighbors();
    }

    /*
     * Returns all currently occupied coordinates.
     */
    public Set<HexCoord> getOccupiedCoordinates() {
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
    public HexGrid copy() {
        HexGrid copy = new HexGrid();
        copy.tiles.putAll(this.tiles);
        return copy;
    }

    /*
     * Returns a map containing the current grid contents.
     */
    public Map<HexCoord, TileType> getTiles() {
        return tiles;
    }
}
