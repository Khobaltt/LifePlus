package main.java.ui;

// files
import main.java.core.HexCoord;
import main.java.core.HexGrid;
import main.java.core.TileType;
// import main.java.core.TileColor;

// util
import java.util.EnumMap;
import java.util.Map;

// swing/awt
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Point2D;
import java.awt.geom.Path2D;

// image processing
import java.awt.image.BufferedImage;

public class GridPanel extends JPanel {

    private final double scaleFactor = 1.02f;

    private final HexGrid grid;

    // size of a hexagon from center to vertex
    private double hexSize = 30.0;

    // camera position in axial coordinates
    private double cameraQ = 0.0;
    private double cameraR = 0.0;

    // tile currently placed by left-click
    private TileType selectedTile = TileType.CONWAY;

    // brush type and size
    // private BrushType brushType = ;
    // private TileColor brushColor = new TileColor(255, 255, 255);
    // private int brushSize = 1;

    // mouse state for panning
    private Point lastMousePosition;
    private boolean panning = false;

    // image caching
    private BufferedImage gridBackground;
    private BufferedImage emptyHexImage;
    private final Map<TileType, BufferedImage> tileImages = new EnumMap<>(TileType.class);
    private double cachedHexSize = -1;

    public GridPanel(HexGrid grid) {
        this.grid = grid;

        setBackground(new Color(30, 30, 30));
        setFocusable(true);

        setupKeyboardShortcuts();
        setupMouseListeners();
    }

    /*
     * Draws the grid
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (getWidth() <= 0 || getHeight() <= 0) { return; }
        ensureHexImages();
        if (hexSize > 15) { ensureGridBackground(); }

        Graphics2D g2 = (Graphics2D) g.create();

        try {
            if (hexSize > 15) {
                g2.drawImage(gridBackground, 0, 0, null);
            }

            drawTiles(g2);
        } finally {
            g2.dispose();
        }

        // // makes hexagon edges look smoother.
        // g2.setRenderingHint(
        //     RenderingHints.KEY_ANTIALIASING,
        //     RenderingHints.VALUE_ANTIALIAS_ON
        // );

        // if (hexSize > 15) { drawGrid(g2); }
        // drawTiles(g2);

        // g2.dispose();
    }

    /*
     * Draws the visible hexagonal grid
     */
    private void drawGrid(Graphics2D g2) {

        HexCoord[] corners = {
            pixelToHex(0, 0),
            pixelToHex(getWidth(), 0),
            pixelToHex(0, getHeight()),
            pixelToHex(getWidth(), getHeight())
        };

        int minQ = Integer.MAX_VALUE;
        int maxQ = Integer.MIN_VALUE;
        int minR = Integer.MAX_VALUE;
        int maxR = Integer.MIN_VALUE;

        for (HexCoord corner : corners) {
            minQ = Math.min(minQ, corner.q);
            maxQ = Math.max(maxQ, corner.q);
            minR = Math.min(minR, corner.r);
            maxR = Math.max(maxR, corner.r);
        }

        // Extra cells cover hexagons partly visible at the panel edges.
        for (int q = minQ - 2; q <= maxQ + 2; q++) {
            for (int r = minR - 2; r <= maxR + 2; r++) {
                HexCoord coord = new HexCoord(q, r);
                Point2D center = hexToPixel(coord);

                if (isVisible(center)) {
                    drawHexImage(g2, emptyHexImage, center);
                }
            }
        }

        // // get anchor coordinates of the screen
        // HexCoord topLeft = pixelToHex(0, 0);
        // HexCoord bottomRight = pixelToHex(getWidth(), getHeight());

        // // large enough to cover the visible screen.
        // int minQ = topLeft.q;
        // int maxQ = bottomRight.q;
        // int minR = bottomRight.r;
        // int maxR = topLeft.r;
        // // int range = 20; //calculateVisibleRange();

        // for (int q = minQ; q <= maxQ; q++) {
        //     for (int r = minR; r <= maxR; r++) {

        //         HexCoord coord = new HexCoord(
        //             (int) cameraQ + q,
        //             (int) cameraR + r
        //         );

        //         Point2D center = hexToPixel(coord);

        //         // don't bother drawing hexagons outside the screen.
        //         if (!isVisible(center)) { continue; }

        //         drawHex(g2, center, new Color(65, 65, 65));
        //     }
        // }
    }

    /****************************************************************************************************************************
     * Image-based hexagon rendering
     ****************************************************************************************************************************/
    private void ensureHexImages() {
        if (cachedHexSize == hexSize) {
            return;
        }

        emptyHexImage = createHexImage(new Color(65, 65, 65));

        tileImages.clear();
        for (TileType tile : TileType.values()) {
            tileImages.put(tile, createHexImage(getTileColor(tile))); // createHexImage(getTileColor(tile)));
        }

        cachedHexSize = hexSize;
        gridBackground = null;
    }

    private BufferedImage createHexImage(Color fillColor) {
        // Padding keeps the outline and antialiasing inside the image.
        int width = (int) Math.ceil(Math.sqrt(3) * hexSize) + 4;
        int height = (int) Math.ceil(2 * hexSize) + 4;

        BufferedImage image = new BufferedImage(
            width, height, BufferedImage.TYPE_INT_ARGB
        );

        Graphics2D g2 = image.createGraphics();
        try {
            g2.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
            );

            Polygon hexagon = createHexagon(width / 2.0, height / 2.0);

            g2.setColor(fillColor);
            g2.fillPolygon(hexagon);

            double opacity = Math.max(
                0, Math.min(1, (hexSize - 15) / 15.0)
            );

            g2.setColor(new Color(90, 90, 90, (int) (255 * opacity)));
            g2.drawPolygon(hexagon);
        } finally {
            g2.dispose();
        }

        return image;
    }

    private void drawHexImage(
        Graphics2D g2,
        BufferedImage image,
        Point2D center
    ) {
        int x = (int) Math.round(center.getX() - image.getWidth() / 2.0);
        int y = (int) Math.round(center.getY() - image.getHeight() / 2.0);

        g2.drawImage(image, x, y, null);
    }

    private void ensureGridBackground() {
        if (gridBackground != null
            && gridBackground.getWidth() == getWidth()
            && gridBackground.getHeight() == getHeight()) {
            return;
        }

        gridBackground = new BufferedImage(
            getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB
        );

        Graphics2D g2 = gridBackground.createGraphics();
        try {
            g2.setColor(getBackground());
            g2.fillRect(0, 0, getWidth(), getHeight());

            drawGrid(g2);
        } finally {
            g2.dispose();
        }
    }

    // private BufferedImage imageForColor(Color color) {
    //     return tileImages.computeIfAbsent(
    //         color.getRGB(),
    //         key -> createHexImage(color)
    //     );
    // }

    /****************************************************************************************************************************
     * TO BE REPLACED: Repaint-based hexagon rendering
     ****************************************************************************************************************************/

    /*
     * Draws all occupied tiles in the HexGrid
     */
    private void drawTiles(Graphics2D g2) {

        for (HexCoord coord : grid.getOccupiedCoordinates()) {
        Point2D center = hexToPixel(coord);

        if (!isVisible(center)) {
            continue;
        }

        BufferedImage image = tileImages.get(grid.get(coord));
        drawHexImage(g2, image, center);
    }

        // for (HexCoord coord : grid.getOccupiedCoordinates()) {

        //     Point2D center = hexToPixel(coord);

        //     if (!isVisible(center)) {
        //         continue;
        //     }

        //     TileType tile = grid.get(coord);

        //     drawHex(
        //         g2,
        //         center,
        //         getTileColor(tile)
        //     );
        // }
    }

    /*
     * Draws one hexagon centered at a pixel location
     */
    // private void drawHex(
    //     Graphics2D g2,
    //     Point2D center,
    //     Color color
    // ) {

    //     Polygon hexagon = createHexagon(
    //         center.getX(),
    //         center.getY()
    //     );

    //     g2.setColor(color);
    //     g2.fillPolygon(hexagon);

    //     g2.setColor(new Color(
    //         90, 90, 90, 
    //         (int) (255 * (hexSize > 30 ? 1 : Math.max((hexSize - 15) / 15.0, 0)))
    //     ));
    //     g2.drawPolygon(hexagon);
    // }

    /*
     * Creates a pointy-top hexagon
     */
    private Polygon createHexagon(double x, double y) {

        Polygon polygon = new Polygon();

        for (int i = 0; i < 6; i++) {

            double angle =
                Math.toRadians(60 * i - 30);

            int px = (int) Math.round(
                x + hexSize * Math.cos(angle)
            );

            int py = (int) Math.round(
                y + hexSize * Math.sin(angle)
            );

            polygon.addPoint(px, py);
        }

        return polygon;
    }

    /****************************************************************************************************************************
     * Coordinate calculation functions
     ****************************************************************************************************************************/

    /*
     * Converts axial hex coordinates to pixel coordinates
     * This uses pointy-top hexagons using +q: right, +r: up-right
     */
    private Point2D hexToPixel(HexCoord coord) {

        double q = coord.q;
        double r = coord.r;

        double x = hexSize * Math.sqrt(3) * (q + r / 2.0);
        double y = hexSize * -3.0 / 2 * r;

        // move the camera origin to the center of the panel.
        x -= hexSize * 1.5 * cameraR;
        y -= hexSize * Math.sqrt(3) * cameraQ;

        x += getWidth() / 2.0;
        y += getHeight() / 2.0;

        return new Point2D.Double(x, y);
    }

    /*
     * Converts a pixel position to the nearest hex coordinate
     * This uses pointy-top hexagons using +q: right, +r: up-right
     */
    private HexCoord pixelToHex(double x, double y) {

        // move the origin back to the camera.
        x -= getWidth() / 2.0;
        y -= getHeight() / 2.0;

        x += hexSize * 1.5 * cameraR;
        y += hexSize * Math.sqrt(3) * cameraQ;

        // convert pixel coordinates to fractional axial coordinates.
        double q = (x / Math.sqrt(3) + y / 3.0) / hexSize;
        double r = (-2.0 / 3 * y) / hexSize;

        return roundAxialCoordinates(q, r);
    }

    /*
     * Rounds fractional axial coordinates to the nearest hex
     */
    private HexCoord roundAxialCoordinates(double q, double r) {

        double s = -q - r;

        int rq = (int) Math.round(q);
        int rr = (int) Math.round(r);
        int rs = (int) Math.round(s);

        double qDifference = Math.abs(rq - q);
        double rDifference = Math.abs(rr - r);
        double sDifference = Math.abs(rs - s);

        if (qDifference > rDifference && qDifference > sDifference) { rq = -rr - rs; } 
        else if (rDifference > sDifference) { rr = -rq - rs; }

        return new HexCoord(rq, rr);
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

                cameraQ -= dx / (hexSize * Math.sqrt(3));
                cameraR -= dy / (hexSize * 1.5);

                lastMousePosition = current;

                gridBackground = null;
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
    }

    /****************************************************************************************************************************
     * Actions
     ****************************************************************************************************************************/

    private void zoom(boolean zoomIn) {
        double oldSize = hexSize;

        if (zoomIn) { hexSize *= scaleFactor; }
        else { hexSize /= scaleFactor; }

        // prevent unreasonable zoom levels
        hexSize = Math.max(5.0, Math.min(hexSize, 150.0));

        // if (oldSize != hexSize) { repaint(); }
        if (oldSize != hexSize) {
            gridBackground = null;
            repaint();
        }
    }

    /*
     * Places the currently selected tile
     */
    private void placeTile(int x, int y) {

        HexCoord coord = pixelToHex(x, y);
        grid.set(coord, selectedTile);

        repaint();
    }

    /*
     * Removes the tile at the clicked position
     */
    private void removeTile(int x, int y) {

        HexCoord coord = pixelToHex(x, y);
        grid.remove(coord);

        repaint();
    }

    /*
     * Sets which tile left-click will place
     */
    public void setSelectedTile(TileType tile) {

        if (tile == null) { throw new IllegalArgumentException("Selected tile cannot be null."); }
        selectedTile = tile;
    }

    /*
     * Returns the currently selected tile
     */
    public TileType getSelectedTile() { return selectedTile; }

    /*
     * Returns a color for each tile type
     */
    private Color getTileColor(TileType tile) {

        return switch (tile) {
            case CONWAY ->
                new Color(255, 255, 255);
            case WALL ->
                new Color(100, 100, 100);
            case PLANT ->
                new Color(0, 255, 0);
        };
    }

    /*
     * Checks whether a pixel coordinate is within or near the visible panel.
     */
    private boolean isVisible(Point2D point) {

        double margin = hexSize * 2;

        return point.getX() >= -margin
            && point.getX() <= getWidth() + margin
            && point.getY() >= -margin
            && point.getY() <= getHeight() + margin;
    }
}
