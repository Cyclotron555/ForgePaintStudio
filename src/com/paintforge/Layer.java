package com.paintforge;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public class Layer {
    private BufferedImage image;
    private Graphics2D g2;
    private boolean visible = true;  // Toggles layer visibility

    public Layer(BufferedImage image, Graphics2D g2) {
        this.image = image;
        this.g2 = g2;
    }

    public BufferedImage getImage() {
        return image;
    }

    public Graphics2D getGraphics2D() {
        return g2;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }
}
