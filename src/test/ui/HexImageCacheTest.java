/* 
 * Running:
 * javac -d out test/ui/HexImageCacheTest.java
 * java -cp out test.ui.HexImageCacheTest
 */

package test.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/** Standalone visual test: three cached images, each drawn three times. */
public final class HexImageCacheTest extends JPanel {
    private static final int HEX_RADIUS = 42;
    private static final int IMAGE_WIDTH =
        (int) Math.ceil(Math.sqrt(3) * HEX_RADIUS) + 6;
    private static final int IMAGE_HEIGHT = HEX_RADIUS * 2 + 6;
    private static final int GAP = 16;
    private static final int LABEL_WIDTH = 140;
    private static final int TOP = 60;

    // Created once per panel, never inside paintComponent().
    private final BufferedImage emptyHex = createHexImage(null);
    private final BufferedImage whiteHex = createHexImage(new Color(255, 255, 255));
    private final BufferedImage redHex = createHexImage(new Color(255, 0, 0));

    public HexImageCacheTest() {
        setBackground(new Color(30, 30, 30));
        setPreferredSize(new Dimension(
            LABEL_WIDTH + 3 * (IMAGE_WIDTH + GAP) + GAP,
            TOP + 3 * (IMAGE_HEIGHT + GAP) + GAP
        ));
    }

    private static BufferedImage createHexImage(Color fill) {
        BufferedImage image = new BufferedImage(
            IMAGE_WIDTH, IMAGE_HEIGHT, BufferedImage.TYPE_INT_ARGB
        );
        Graphics2D g2 = image.createGraphics();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

            double centerX = IMAGE_WIDTH / 2.0;
            double centerY = IMAGE_HEIGHT / 2.0;
            Path2D.Double hexagon = new Path2D.Double();
            for (int i = 0; i < 6; i++) {
                double angle = Math.toRadians(60 * i - 30);
                double x = centerX + HEX_RADIUS * Math.cos(angle);
                double y = centerY + HEX_RADIUS * Math.sin(angle);
                if (i == 0) {
                    hexagon.moveTo(x, y);
                } else {
                    hexagon.lineTo(x, y);
                }
            }
            hexagon.closePath();

            // null means an empty hexagon with a transparent interior.
            if (fill != null) {
                g2.setColor(fill);
                g2.fill(hexagon);
            }
            g2.setColor(new Color(150, 150, 150));
            g2.draw(hexagon);
        } finally {
            g2.dispose();
        }
        return image;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setColor(Color.WHITE);
            g2.drawString("3 cached images; each image reused 3 times", 20, 30);
            drawRow(g2, emptyHex, "Empty", TOP);
            drawRow(g2, whiteHex, "White (255,255,255)", TOP + IMAGE_HEIGHT + GAP);
            drawRow(g2, redHex, "Red (255,0,0)", TOP + 2 * (IMAGE_HEIGHT + GAP));
        } finally {
            g2.dispose();
        }
    }

    private void drawRow(Graphics2D g2, BufferedImage image, String label, int y) {
        g2.setColor(Color.WHITE);
        g2.drawString(label, 12, y + IMAGE_HEIGHT / 2);
        for (int column = 0; column < 3; column++) {
            int x = LABEL_WIDTH + column * (IMAGE_WIDTH + GAP);
            g2.drawImage(image, x, y, null);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame window = new JFrame("LifePlus - Hex Image Cache Test");
            window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            window.setContentPane(new HexImageCacheTest());
            window.pack();
            window.setLocationRelativeTo(null);
            window.setVisible(true);
        });
    }
}
