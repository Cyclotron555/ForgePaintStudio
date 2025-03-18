package com.paintforge;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;

public class ColorWheel extends JPanel {
    private BufferedImage wheelImage;
    private int wheelRadius;
    private float brightness = 1.0f;   // user-controlled brightness
    private Color selectedColor = Color.WHITE;

    // Track the hover position (null if outside the wheel)
    private Point hoverPoint = null;

    public ColorWheel(int radius) {
        this.wheelRadius = radius;
        setPreferredSize(new Dimension(radius * 2, radius * 2));

        generateWheelImage();

        // Listen for clicks/drags to pick color
        MouseAdapter adapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                pickColor(e.getPoint());
            }
            @Override
            public void mouseDragged(MouseEvent e) {
                pickColor(e.getPoint());
            }
        };
        addMouseListener(adapter);
        addMouseMotionListener(adapter);

        // Listen for mouse movement to show the small hover circle
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                updateHover(e.getPoint());
            }
            @Override
            public void mouseDragged(MouseEvent e) {
                updateHover(e.getPoint());
            }
        });
    }

    /**
     * Generates the hue/sat wheel image using the current brightness.
     */
    private void generateWheelImage() {
        wheelImage = new BufferedImage(wheelRadius * 2, wheelRadius * 2, BufferedImage.TYPE_INT_ARGB);
        for (int y = -wheelRadius; y < wheelRadius; y++) {
            for (int x = -wheelRadius; x < wheelRadius; x++) {
                double angle = Math.atan2(y, x) + Math.PI;
                double distance = Point2D.distance(0, 0, x, y);

                if (distance <= wheelRadius) {
                    float hue = (float) (angle / (2 * Math.PI));
                    if (hue < 0) {
                        hue += 1.0f; // ensure hue in [0..1]
                    }
                    float saturation = Math.min((float) (distance / wheelRadius), 1.0f);

                    // Use the current brightness
                    int rgb = Color.HSBtoRGB(hue, saturation, brightness);
                    wheelImage.setRGB(x + wheelRadius, y + wheelRadius, rgb);
                } else {
                    // Outside the wheel => transparent
                    wheelImage.setRGB(x + wheelRadius, y + wheelRadius, 0x00000000);
                }
            }
        }
    }

    /**
     * Attempt to pick a color from the hue/sat wheel at point p.
     */
    private void pickColor(Point p) {
        int px = p.x;
        int py = p.y;
        if (!isInBounds(px, py)) {
            return;
        }

        int dx = px - wheelRadius;
        int dy = py - wheelRadius;
        double dist = Math.sqrt(dx * dx + dy * dy);
        if (dist <= wheelRadius) {
            // Inside the circle, pick the pixel color
            int argb = wheelImage.getRGB(px, py);
            selectedColor = new Color(argb, true);

            repaint();
            firePropertyChange("selectedColor", null, selectedColor);
        }
    }

    /**
     * Update hoverPoint if the mouse is inside the wheel, else null.
     */
    private void updateHover(Point p) {
        int px = p.x;
        int py = p.y;
        if (!isInBounds(px, py)) {
            hoverPoint = null;
            repaint();
            return;
        }

        int dx = px - wheelRadius;
        int dy = py - wheelRadius;
        double dist = Math.sqrt(dx * dx + dy * dy);

        if (dist <= wheelRadius) {
            hoverPoint = p;  // inside the circle
        } else {
            hoverPoint = null;
        }
        repaint();
    }

    /**
     * Simple bounds check for the wheelImage dimension.
     */
    private boolean isInBounds(int px, int py) {
        return px >= 0 && py >= 0
                && px < wheelImage.getWidth()
                && py < wheelImage.getHeight();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Draw the hue/sat wheel
        g.drawImage(wheelImage, 0, 0, null);

        // Optional: draw a small white ring in the center
        g.setColor(Color.WHITE);
        g.drawOval(wheelRadius - 5, wheelRadius - 5, 10, 10);


        // Draw a 5 px white circle at the hover point if inside
        if (hoverPoint != null) {
            g.setColor(Color.WHITE);
            g.fillOval(hoverPoint.x - 2, hoverPoint.y - 2, 10, 10);
        }
    }

    // ----------------------------------------------------------------
    // GETTERS / SETTERS
    // ----------------------------------------------------------------

    /**
     * 0 <= brightness <= 1; re-generates the wheel with new brightness.
     */
    public void setBrightness(float b) {
        this.brightness = Math.max(0, Math.min(1, b));
        generateWheelImage();
        repaint();
    }

    public float getBrightness() {
        return brightness;
    }

    public Color getSelectedColor() {
        return selectedColor;
    }
}
