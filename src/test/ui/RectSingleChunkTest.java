package test.ui;

// files
import main.java.core.rect.RectCoord;
import main.java.core.rect.RectGrid;
import main.java.ui.rect.RectGridPanel;

// swing/awt
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

// image processing
import java.awt.image.BufferedImage;

public class RectSingleChunkTest {

    // these values match the attached RectGridPanel
    private static final int CHUNK_SIZE = 32;
    private static final double INITIAL_RECT_SIZE = 30.0;
    private static final double SCALE_FACTOR = 1.02f;
    private static final int ZOOM_OUT_STEPS = 56;
    private static final int CAMERA_STEPS = 16;
    private static final int PANEL_SIZE = 640;

    /*
     * Runs the checks and opens a standalone visual test window
     * Use --headless to run the checks without displaying a window
     */
    public static void main(String[] args) throws Exception {

        boolean headless = args.length == 1 && "--headless".equals(args[0]);
        if (headless) { System.setProperty("java.awt.headless", "true"); }

        if (!headless && GraphicsEnvironment.isHeadless()) {
            throw new IllegalStateException(
                "A desktop is required to display the window. Use --headless for checks only."
            );
        }

        // create and paint Swing components on the event dispatch thread
        SwingUtilities.invokeAndWait(() -> runTest(headless));
    }

    /****************************************************************************************************************************
     * Test image
     ****************************************************************************************************************************/

    /*
     * Creates a predetermined grayscale image for one chunk
     * Bottom-left is black, top-right is white, and the other corners are gray
     */
    private static BufferedImage createGradient() {

        BufferedImage image = new BufferedImage(
            CHUNK_SIZE, CHUNK_SIZE, BufferedImage.TYPE_INT_RGB
        );

        for (int y = 0; y < CHUNK_SIZE; y++) {
            for (int x = 0; x < CHUNK_SIZE; x++) {

                // increase brightness equally as x and y increase
                int gray = (int) Math.round(
                    255.0 * (x + y) / (2 * (CHUNK_SIZE - 1))
                );

                int rgb = (gray << 16) | (gray << 8) | gray;

                // image rows point downward; cartesian y points upward
                image.setRGB(x, CHUNK_SIZE - 1 - y, rgb);
            }
        }

        return image;
    }

    /****************************************************************************************************************************
     * Test setup
     ****************************************************************************************************************************/

    private static void runTest(boolean headless) {

        BufferedImage gradient = createGradient();
        RectGrid grid = new RectGrid();

        // coordinates 0 through 31 belong entirely to chunk (0, 0)
        for (int y = 0; y < CHUNK_SIZE; y++) {
            for (int x = 0; x < CHUNK_SIZE; x++) {
                Color color = new Color(gradient.getRGB(x, CHUNK_SIZE - 1 - y));
                grid.set(new RectCoord(x, y), color);
            }
        }

        // the panel constructor imports the model into its image cache
        RectGridPanel panel = new RectGridPanel(grid);
        panel.setPreferredSize(new Dimension(PANEL_SIZE, PANEL_SIZE));
    }
}