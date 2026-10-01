package main.java.ui;

import main.java.core.HexCoord;
import main.java.core.HexGrid;
import main.java.core.TileType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Point2D;

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

    // mouse state for panning
    private Point lastMousePosition;
    private boolean panning = false;

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

        Graphics2D g2 = (Graphics2D) g.create();

        // makes hexagon edges look smoother.
        g2.setRenderingHint(
            RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON
        );

        if (hexSize > 15) { drawGrid(g2); }
        drawTiles(g2);

        g2.dispose();
    }

    /*
     * Draws the visible hexagonal grid
     */
    private void drawGrid(Graphics2D g2) {

        // get anchor coordinates of the screen
        HexCoord topLeft = pixelToHex(0, 0);
        HexCoord bottomRight = pixelToHex(getWidth(), getHeight());

        // large enough to cover the visible screen.
        int minQ = topLeft.q;
        int maxQ = bottomRight.q;
        int minR = bottomRight.r;
        int maxR = topLeft.r;
        // int range = 20; //calculateVisibleRange();

        for (int q = minQ; q <= maxQ; q++) {
            for (int r = minR; r <= maxR; r++) {

                HexCoord coord = new HexCoord(
                    (int) cameraQ + q,
                    (int) cameraR + r
                );

                Point2D center = hexToPixel(coord);

                // don't bother drawing hexagons outside the screen.
                if (!isVisible(center)) { continue; }

                drawHex(g2, center, new Color(65, 65, 65));
            }
        }
    }

    /*
     * Draws all occupied tiles in the HexGrid
     */
    private void drawTiles(Graphics2D g2) {

        for (HexCoord coord : grid.getOccupiedCoordinates()) {

            Point2D center = hexToPixel(coord);

            if (!isVisible(center)) {
                continue;
            }

            TileType tile = grid.get(coord);

            drawHex(
                g2,
                center,
                getTileColor(tile)
            );
        }
    }

    /*
     * Draws one hexagon centered at a pixel location
     */
    private void drawHex(
        Graphics2D g2,
        Point2D center,
        Color color
    ) {

        Polygon hexagon = createHexagon(
            center.getX(),
            center.getY()
        );

        g2.setColor(color);
        g2.fillPolygon(hexagon);

        g2.setColor(new Color(
            90, 90, 90, 
            (int) (255 * (hexSize > 30 ? 1 : Math.max((hexSize - 15) / 15.0, 0)))
        ));
        g2.drawPolygon(hexagon);
    }

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

    private void zoom(boolean zoomIn) {
        double oldSize = hexSize;

        if (zoomIn) { hexSize *= scaleFactor; }
        else { hexSize /= scaleFactor; }

        // prevent unreasonable zoom levels
        hexSize = Math.max(5.0, Math.min(hexSize, 150.0));

        if (oldSize != hexSize) { repaint(); }
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
