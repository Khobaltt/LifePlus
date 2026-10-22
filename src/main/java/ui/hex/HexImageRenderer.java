package main.java.ui.hex;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.io.IOException;
import java.nio.file.Path;

import javax.imageio.ImageIO;

public class HexImageRenderer {

    // store the original image and reuse one filtered image
    private final BufferedImage originalImage;
    private final BufferedImage filteredImage;

    public HexImageRenderer(Path imagePath) throws IOException {

        // import the hexagon image
        BufferedImage importedImage = ImageIO.read(imagePath.toFile());

        if (importedImage == null) {
            throw new IOException("Unable to read image: " + imagePath);
        }

        // use an image format with red, green, blue, and alpha channels
        originalImage = new BufferedImage(
            importedImage.getWidth(),
            importedImage.getHeight(),
            BufferedImage.TYPE_INT_ARGB
        );

        Graphics2D graphics = originalImage.createGraphics();

        try {
            graphics.drawImage(importedImage, 0, 0, null);
        } finally {
            graphics.dispose();
        }

        // create the output buffer once
        filteredImage = new BufferedImage(
            originalImage.getWidth(),
            originalImage.getHeight(),
            BufferedImage.TYPE_INT_ARGB
        );
    }

    // call this method from the Swing event dispatch thread
    public void draw(
        Graphics2D graphics,
        int red,
        int green,
        int blue,
        int x,
        int y,
        int width,
        int height
    ) {

        // check that each color value is valid
        if (red < 0 || red > 255
            || green < 0 || green > 255
            || blue < 0 || blue > 255) {
            throw new IllegalArgumentException(
                "RGB values must be between 0 and 255"
            );
        }

        // scale the color channels and preserve the alpha channel
        float[] scales = {
            red / 255.0f,
            green / 255.0f,
            blue / 255.0f,
            1.0f
        };

        float[] offsets = {0.0f, 0.0f, 0.0f, 0.0f};

        RescaleOp filter = new RescaleOp(scales, offsets, null);

        // always filter the original image to avoid accumulating changes
        filter.filter(originalImage, filteredImage);

        // draw before reusing the output buffer for the next tile
        graphics.drawImage(
            filteredImage,
            x,
            y,
            width,
            height,
            null
        );
    }
}