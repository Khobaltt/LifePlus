package main.java.ui.rect;

import main.java.core.rect.RectCoord;
import main.java.core.rect.RectGrid;

// util
import java.util.HashMap;
import java.util.Map;

// swing/awt
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Point2D;
import java.awt.geom.AffineTransform;

// image processing
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

public class RectGridPanel extends JPanel {

    // initial values
    private final double INIT_SCALE_FACTOR = 1.1f;
    private final double INIT_PANNING_SPEED = 30.0f;
    private final double INIT_RECT_SIZE = 30.0f;
    private final double INIT_CAM_X = 0.0f;
    private final double INIT_CAM_Y = 0.0f;
    private final Color INIT_BRUSH_COLOR = new Color(255, 255, 255);
    private final int INIT_BRUSH_SIZE = 1;
    private final Color INIT_BACKGROUND_COLOR = new Color(0, 0, 0);
    private final Color INIT_GRID_COLOR = new Color(90, 90, 90);
    private final double INIT_GRID_TRANSPARENCY_A = 5f;
    private final double INIT_GRID_TRANSPARENCY_B = 15f;
    private static final int INIT_CHUNK_SIZE = 32;

    // debug settings
    private boolean highlightOrigin = true;
    private boolean trackMousePosition = true;
    private Color highlightColor = new Color(255, 255, 0);

    // grid that is rendered
    private final RectGrid grid;
    private Color gridColor = INIT_GRID_COLOR;
    private double gridTransparencyA = INIT_GRID_TRANSPARENCY_A;
    private double gridTransparencyB = INIT_GRID_TRANSPARENCY_B;

    // size of a hexagon from center to vertex
    private double rectSize = INIT_RECT_SIZE;

    // camera position in cartesian coordinates
    private double cameraX = INIT_CAM_X;
    private double cameraY = INIT_CAM_Y;

    // camera settings
    private final double scaleFactor = INIT_SCALE_FACTOR;
    private final double panningSpeed = INIT_PANNING_SPEED;

    // brush type and size
    // private BrushType brushType = BrushType.SET;
    private Color selectedColor = INIT_BRUSH_COLOR;
    private int brushSize = INIT_BRUSH_SIZE;

    // mouse state for panning
    private Point lastMousePosition;
    private boolean panning = false;

    // cache one image pixel per tile, grouped into n-by-n chunks
    private static final int chunkSize = INIT_CHUNK_SIZE;
    private final Map<Long, TileChunk> tileChunks = new HashMap<>();

    /*
     *
     */
    private static class TileChunk {
        final BufferedImage image = new BufferedImage(chunkSize, chunkSize, BufferedImage.TYPE_INT_ARGB);

        // array stores the image
        final int[] pixels = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();

        // count visible pixels so an empty chunk can be discarded
        int visiblePixels = 0;
    }

    // image caching
    // private BufferedImage gridBackground;
    // private BufferedImage emptyRectImage;
    // private final Map<Color, BufferedImage> tileImages = new HashMap<>();
    // private double cachedHexSize = -1;

    public RectGridPanel(RectGrid grid) {
        if (grid == null) { throw new IllegalArgumentException("Grid cannot be null."); }
        this.grid = grid;

        setBackground(INIT_BACKGROUND_COLOR);
        setOpaque(true);
        setFocusable(true);

        setupKeyboardShortcuts();
        setupMouseListeners();

        // import existing model once
        refreshTiles();
    }

    /****************************************************************************************************************************
     * Image-based rectangle rendering
     ****************************************************************************************************************************/

    /*
     * Rebuilds the display cache after direct changes to RectGrid
     * Call once after a batch, on the Swing event dispatch thread
     */
    public void refreshTiles() {
        tileChunks.clear();

        for (RectCoord coord : grid.getOccupiedCoordinates()) {
            updateCachedTile(coord, grid.get(coord));
        }

        repaint();
    }

    /*
     * Updates one cached pixel; null represents an empty tile
     */
    private void updateCachedTile(RectCoord coord, Color color) {

        // floor division keeps negative coordinates in the correct chunks
        int chunkX = Math.floorDiv(coord.x, chunkSize);
        int chunkY = Math.floorDiv(coord.y, chunkSize);
        long key = chunkKey(chunkX, chunkY);

        // transparent pixels reveal the panel background
        int rgb = color == null || color.getAlpha() == 0 ? 0 : color.getRGB();
        TileChunk chunk = tileChunks.get(key);

        if (chunk == null) {
            if (rgb == 0) { return; }
            chunk = new TileChunk();
            tileChunks.put(key, chunk);
        }

        int localX = Math.floorMod(coord.x, chunkSize);
        int localY = chunkSize - 1 - Math.floorMod(coord.y, chunkSize);
        int index = localY * chunkSize + localX;

        // image rows point downward, so reverse the cartesian y coordinate
        if (chunk.pixels[index] == 0 && rgb != 0) { chunk.visiblePixels++; }
        if (chunk.pixels[index] != 0 && rgb == 0) { chunk.visiblePixels--; }
        chunk.pixels[index] = rgb;

        if (chunk.visiblePixels == 0) { tileChunks.remove(key); }
    }

    /*
     * Combines two signed chunk coordinates into one unique map key
     */
    private long chunkKey(int x, int y) { return ((long) x << 32) | (y & 0xFFFFFFFFL); }

    /*
     * Draws cached chunks that intersect the panel's repaint area
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (getWidth() <= 0 || getHeight() <= 0) { return; }

        Graphics2D g2 = (Graphics2D) g.create();

        try {

            // enlarge tile pixels without blending neighboring colors
            g2.setRenderingHint(
                RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR
            );

            drawTiles(g2);
            if (rectSize > gridTransparencyA) { drawGrid(g2); }

        } finally { g2.dispose(); }
    }

    /*
     * Draws one image per visible occupied chunk, instead of one shape per tile
     */
    private void drawTiles(Graphics2D g2) {
        Rectangle clip = g2.getClipBounds();
        if (clip == null) { clip = new Rectangle(0, 0, getWidth(), getHeight()); }
        if (clip.isEmpty()) { return; }

        RectCoord topLeft = pixelToRect(clip.getMinX(), clip.getMinY());
        RectCoord bottomRight = pixelToRect(clip.getMaxX(), clip.getMaxY());

        int minChunkX = Math.floorDiv(topLeft.x, chunkSize);
        int maxChunkX = Math.floorDiv(bottomRight.x, chunkSize);
        int minChunkY = Math.floorDiv(bottomRight.y, chunkSize);
        int maxChunkY = Math.floorDiv(topLeft.y, chunkSize);

        double originX = getWidth() / 2.0 - cameraX * rectSize;
        double originY = getHeight() / 2.0 + cameraY * rectSize;

        for (int chunkY = minChunkY; chunkY <= maxChunkY; chunkY++) {
            for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
                TileChunk chunk = tileChunks.get(chunkKey(chunkX, chunkY));
                if (chunk == null) { continue; }

                // integer grid coordinates refer to tile centers
                double left = originX + ((double) chunkX * chunkSize - 0.5) * rectSize;
                double top = originY - ((double) chunkY * chunkSize + chunkSize - 0.5) * rectSize;

                // apply the camera and zoom without rebuilding the image
                AffineTransform transform = new AffineTransform();
                transform.translate(left, top);
                transform.scale(rectSize, rectSize);
                g2.drawImage(chunk.image, transform, null);
            }
        }
    }

    /*
     * Draws square grid lines only when individual tiles are large enough
     */
    private void drawGrid(Graphics2D g2) {
        double originX = getWidth() / 2.0 - cameraX * rectSize;
        double originY = getHeight() / 2.0 + cameraY * rectSize;

        // normalize the first boundary so it is just outside the panel
        double firstX = (originX - rectSize / 2.0) % rectSize;
        double firstY = (originY - rectSize / 2.0) % rectSize;
        if (firstX > 0) { firstX -= rectSize; }
        if (firstY > 0) { firstY -= rectSize; }

        if (rectSize > gridTransparencyA && rectSize < gridTransparencyB) {
            Color transparentGridColor = new Color(
                gridColor.getRed(), 
                gridColor.getGreen(), 
                gridColor.getBlue(), 
                (int) Math.round((rectSize - gridTransparencyA) / (gridTransparencyB - gridTransparencyA) * 255)
            );
            g2.setColor(transparentGridColor);
        } else { g2.setColor(gridColor); }

        // draw grid lines
        for (double x = firstX; x < getWidth() && x != originX; x += rectSize) {
            int screenX = (int) Math.round(x);
            g2.drawLine(screenX, 0, screenX, getHeight());
        }
        for (double y = firstY; y < getHeight() && y != originY; y += rectSize) {
            int screenY = (int) Math.round(y);
            g2.drawLine(0, screenY, getWidth(), screenY);
        }

        // draw axis lines
        if (highlightOrigin) { g2.setColor(highlightColor); }
        g2.drawLine((int) Math.round(originX), 0, (int) Math.round(originX), getHeight());
        g2.drawLine(0, (int) Math.round(originY), getWidth(), (int) Math.round(originY));
    }

    /*
     * Updates one tile in both the model and cache without requesting a repaint
     * Call on the Swing event dispatch thread, then repaint once after a batch
     */
    public void setTile(RectCoord coord, Color color) {
        if (coord == null || color == null) {
            throw new IllegalArgumentException("Coordinate and color cannot be null.");
        }

        grid.set(coord, color);
        updateCachedTile(coord, color);
    }

    /*
     * Removes one tile from both the model and cache
     */
    public void clearTile(RectCoord coord) {
        if (coord == null) { throw new IllegalArgumentException("Coordinate cannot be null."); }

        grid.remove(coord);
        updateCachedTile(coord, null);
    }
    
    /****************************************************************************************************************************
     * Coordinate calculation functions
     ****************************************************************************************************************************/

    /*
     * Converts cartesian tile coordinates to screen coordinates
     * Positive x points right and positive y points upward
     */
    private Point2D rectToPixel(RectCoord coord) {

        double x = rectSize * coord.x;
        double y = -rectSize * coord.y; // cartesian y increases upwards, screen y increases downwards

        // move the camera origin to the center of the panel
        x -= rectSize * cameraX;
        y += rectSize * cameraY;

        x += getWidth() / 2.0;
        y += getHeight() / 2.0;

        return new Point2D.Double(x, y);
    }

    /*
     * Converts a screen position to the nearest square tile
     * Positive x points right and positive y points upward
     */
    private RectCoord pixelToRect(double x, double y) {

        x = (x - getWidth() / 2.0) / rectSize + cameraX;
        y = (getHeight() / 2.0 - y) / rectSize + cameraY;

        return roundCartesianCoordinates(x, y);

        // // move the origin back to the camera
        // x -= getWidth() / 2.0;
        // y -= getHeight() / 2.0;

        // x += rectSize * cameraX;
        // y += rectSize * cameraY;

        // // convert screen offsets to fractional cartesian tile coordinates
        // x /= rectSize;
        // y /= rectSize;

        // return roundCartesianCoordinates(x, y);
    }

    /*
     * Rounds fractional axial coordinates to the nearest hex
     */
    private RectCoord roundCartesianCoordinates(double x, double y) {

        int rx = (int) Math.round(x);
        int ry = (int) Math.round(y);

        // double qDifference = Math.abs(rq - q);
        // double rDifference = Math.abs(rr - r);
        // double sDifference = Math.abs(rs - s);

        // if (qDifference > rDifference && qDifference > sDifference) { rq = -rr - rs; } 
        // else if (rDifference > sDifference) { rr = -rq - rs; }

        return new RectCoord(rx, ry);
    }

    /****************************************************************************************************************************
     * Input handlers
     ****************************************************************************************************************************/

    /*
     * Handles mouse interaction
     */
    private void setupMouseListeners() {

        addMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {

                if (SwingUtilities.isMiddleMouseButton(e)) {
                    panning = true;
                    lastMousePosition = e.getPoint();
                    return;
                }
                if (SwingUtilities.isLeftMouseButton(e)) { placeTile(e.getX(), e.getY()); }
                if (SwingUtilities.isRightMouseButton(e)) { removeTile(e.getX(), e.getY()); }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (SwingUtilities.isMiddleMouseButton(e)) { panning = false; }
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {

            @Override
            public void mouseDragged(MouseEvent e) {

                if (!panning) { return; }

                Point current = e.getPoint();
                double dx = current.getX() - lastMousePosition.getX();
                double dy = current.getY() - lastMousePosition.getY();

                cameraX -= dx / (rectSize * Math.sqrt(3));
                cameraY -= dy / (rectSize * 1.5);

                lastMousePosition = current;

                // gridBackground = null;
                repaint();
            }
        });

        addMouseWheelListener(e -> {
            zoom(e.getWheelRotation() < 0);
        });
    }

    private void setupKeyboardShortcuts() {
        InputMap inputMap = getInputMap(WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = getActionMap();

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "zoomIn");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "zoomOut");

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_W, 0), "panUp");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_A, 0), "panLeft");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, 0), "panDown");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_D, 0), "panRight");

        actionMap.put("zoomIn", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                zoom(true);
            }
        });
        actionMap.put("zoomOut", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                zoom(false);
            }
        });

        actionMap.put("panUp", new AbstractAction() {
            @Override 
            public void actionPerformed(ActionEvent e) {
                pan(0, 1);
            }
        });
        actionMap.put("panLeft", new AbstractAction() {
            @Override 
            public void actionPerformed(ActionEvent e) {
                pan(-1, 0);
            }
        });
        actionMap.put("panDown", new AbstractAction() {
            @Override 
            public void actionPerformed(ActionEvent e) {
                pan(0, -1);
            }
        });
        actionMap.put("panRight", new AbstractAction() {
            @Override 
            public void actionPerformed(ActionEvent e) {
                pan(1, 0);
            }
        });
    }

    /****************************************************************************************************************************
     * Actions
     ****************************************************************************************************************************/

    private void zoom(boolean zoomIn) {
        double oldSize = rectSize;

        if (zoomIn) { rectSize *= scaleFactor; }
        else { rectSize /= scaleFactor; }

        // prevent unreasonable zoom levels
        rectSize = Math.max(5.0, Math.min(rectSize, 150.0));

        // if (oldSize != rectSize) { repaint(); }
        if (oldSize != rectSize) {
            // gridBackground = null;
            repaint();
        }
    }

    private void pan(double x, double y) {
        cameraX += panningSpeed / rectSize * x;
        cameraY += panningSpeed / rectSize * y;
        repaint();
    }

    /*
     * Places the currently selected tile
     */
    private void placeTile(int x, int y) {
        RectCoord coord = pixelToRect(x, y);
        setTile(coord, selectedColor);
        repaint();
    }

    /*
     * Removes the tile at the clicked position
     */
    private void removeTile(int x, int y) {
        RectCoord coord = pixelToRect(x, y);
        clearTile(coord);
        repaint();
    }

    /*
     * Sets which tile left-click will place
     */
    public void setSelectedTile(Color color) {

        if (color == null) { throw new IllegalArgumentException("Selected tile cannot be null."); }
        selectedColor = color;
    }

    /*
     * Returns the currently selected tile
     */
    public Color getSelectedColor() { return selectedColor; }

    /*
     * Returns a color for each tile type
     */
    // private Color getTileColor(Color color) {

    //     return switch (tile) {
    //         case CONWAY ->
    //             new Color(255, 255, 255);
    //         case WALL ->
    //             new Color(100, 100, 100);
    //         case PLANT ->
    //             new Color(0, 255, 0);
    //     };
    // }

    /*
     * Checks whether a pixel coordinate is within or near the visible panel.
     */
    // private boolean isVisible(Point2D point) {

    //     double margin = rectSize * 2;

    //     return point.getX() >= -margin
    //         && point.getX() <= getWidth() + margin
    //         && point.getY() >= -margin
    //         && point.getY() <= getHeight() + margin;
    // }
}
