package com.paintforge;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.Scrollable;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Stack;

public class PaintCanvas extends JPanel implements Scrollable {
    // Temporary tool flag (for eyedropper, etc.)
    private boolean isTemporaryToolActive = false;
    // Single-layer image & graphics
    private BufferedImage image;
    private Graphics2D g2;
    // Zoom & Pan
    private double zoomFactor = 1.0;
    private double panX = 0, panY = 0;
    private boolean isPanning = false;
    private int lastPanX, lastPanY;
    // Undo/Redo stacks
    private final Stack<BufferedImage> undoStack = new Stack<>();
    private final Stack<BufferedImage> redoStack = new Stack<>();
    // Tools & brush settings
    private Color currentColor = Color.BLACK;
    private int brushSize = 1;
    private String currentTool = "BRUSH";
    private String previousTool = "BRUSH";
    // For freehand brush: track previous coordinates
    private int prevX = -1, prevY = -1;
    // Line tool fields
    private boolean drawingLine = false;
    private int lineStartX, lineStartY;
    private int lineEndX, lineEndY;
    private boolean pixelPerfectMode = true;
    private Timer pixelCorrectionTimer;
    private int lastDrawX = -1, lastDrawY = -1;

    public PaintCanvas() {
        setDoubleBuffered(true);
        setBackground(Color.WHITE);
        initCanvas();

        // Mouse listeners
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isMiddleMouseButton(e)) {
                    isPanning = true;
                    lastPanX = e.getX();
                    lastPanY = e.getY();
                } else {
                    saveState();
                    int cx = screenToCanvasX(e.getX());
                    int cy = screenToCanvasY(e.getY());
                    if ("COLOR_PICKER".equals(currentTool)) {
                        if (cx >= 0 && cy >= 0 && cx < image.getWidth() && cy < image.getHeight()) {
                            int argb = image.getRGB(cx, cy);
                            Color pickedColor = new Color(argb, true);
                            if (!pickedColor.equals(Color.WHITE)) {
                                setBrushColor(pickedColor);
                                System.out.println("Eyedropper picked: " + pickedColor);
                            }
                        }
                    } else if ("BUCKET".equals(currentTool)) {
                        floodFill(cx, cy, currentColor);
                    } else if ("LINE".equals(currentTool)) {
                        drawingLine = true;
                        lineStartX = cx;
                        lineStartY = cy;
                        lineEndX = cx;
                        lineEndY = cy;
                    } else {
                        // For BRUSH/ERASER, initialize previous coordinates
                        prevX = cx;
                        prevY = cy;
                    }
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (SwingUtilities.isMiddleMouseButton(e)) {
                    isPanning = false;
                }
                else if ("BRUSH".equals(currentTool) || "ERASER".equals(currentTool)) {
                    stopPixelCorrection();
                }
                else if ("LINE".equals(currentTool) && drawingLine) {
                    int dx = lineEndX - lineStartX;
                    int dy = lineEndY - lineStartY;
                    double angleDeg = Math.toDegrees(Math.atan2(dy, dx));
                    double snappedAngleDeg = Math.round(angleDeg / 15.0) * 15.0;
                    double length = Math.hypot(dx, dy);
                    double rad = Math.toRadians(snappedAngleDeg);
                    int finalEndX = lineStartX + (int) Math.round(length * Math.cos(rad));
                    int finalEndY = lineStartY + (int) Math.round(length * Math.sin(rad));
                    g2.setColor(currentColor);
                    g2.setStroke(new BasicStroke(brushSize, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    if ("BRUSH".equals(currentTool) && brushSize == 1) {
                        int cx = screenToCanvasX(e.getX());
                        int cy = screenToCanvasY(e.getY());
                        drawPixelPerfectSegment(prevX, prevY, cx, cy, currentColor);
                    }
                    drawingLine = false;
                    revalidate();
                    repaint();
                }
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (isPanning) {
                    int dx = e.getX() - lastPanX;
                    int dy = e.getY() - lastPanY;
                    panX += dx;
                    panY += dy;
                    lastPanX = e.getX();
                    lastPanY = e.getY();
                    repaint();
                    return;
                }

                int cx = screenToCanvasX(e.getX());
                int cy = screenToCanvasY(e.getY());

                // Only allow movement if there's at least a 1-pixel gap
                if (Math.abs(cx - prevX) > 1 || Math.abs(cy - prevY) > 1) {
                    if ("BRUSH".equals(currentTool) && pixelPerfectMode && brushSize == 1) {
                        drawPixelPerfectSegment(prevX, prevY, cx, cy, currentColor);
                    } else {
                        g2.setColor(currentColor);
                        g2.setStroke(new BasicStroke(brushSize, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                        g2.drawLine(prevX, prevY, cx, cy);
                        image.setRGB(cx, cy, currentColor.getRGB());
                    }

                    prevX = cx;
                    prevY = cy;
                    repaint();
                }
            }


        });

        addMouseWheelListener(e -> {
            double factor = (e.getWheelRotation() < 0) ? 1.2 : 0.8;
            zoom(factor, e.getX(), e.getY());
        });

        // Timer to enforce pixel-perfect drawing correction every 10ms
        pixelCorrectionTimer = new Timer(10, e -> {
            if (prevX != -1 && prevY != -1 && lastDrawX != -1 && lastDrawY != -1) {
                drawPixelPerfectSegment(prevX, prevY, lastDrawX, lastDrawY, currentColor);
                revalidate();
                repaint();
            }
        });
        pixelCorrectionTimer.start();

    }


    // ----- Drawing Methods (Pixel Perfect) -----
    /**
     * Draws a 1-pixel-wide line between (x0, y0) and (x1, y1) using a DDA-like algorithm.
     * If skipLastPixel is true, the final pixel is not drawn.
     */
    private void drawPixelPerfectSegment(int x0, int y0, int x1, int y1, Color color) {
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = (x0 < x1) ? 1 : -1;
        int sy = (y0 < y1) ? 1 : -1;
        int err = dx - dy;

        boolean lastMoveWasDiagonal = false;

        while (x0 != x1 || y0 != y1) {
            if (x0 >= 0 && y0 >= 0 && x0 < image.getWidth() && y0 < image.getHeight()) {
                if (!lastMoveWasDiagonal) {
                    image.setRGB(x0, y0, color.getRGB());
                } else {
                    erasePixel(x0, y0); // Remove any misdrawn pixels
                }
            }

            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                x0 += sx;
                lastMoveWasDiagonal = (dy != 0);
            }
            if (e2 < dx) {
                err += dx;
                y0 += sy;
                lastMoveWasDiagonal = (dx != 0);
            }
        }
    }



    /**
     * Prevents forming extra diagonal pixels by removing the last straight pixel.
     */
    private boolean shouldRemovePrevious(int lastX, int lastY, int x, int y, boolean lastMoveWasStraight) {
        // If the last move was straight and the new one is diagonal, remove the last one
        return lastMoveWasStraight && Math.abs(x - lastX) == 1 && Math.abs(y - lastY) == 1;
    }

    // ----- Canvas Initialization -----
    public void initCanvas(int w, int h) {
        image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        g2 = image.createGraphics();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC));
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, w, h);
        zoomFactor = 1.0;

        SwingUtilities.invokeLater(() -> {
            if (getParent() instanceof JViewport) {
                JViewport viewport = (JViewport) getParent();
                int viewWidth = viewport.getWidth();
                int viewHeight = viewport.getHeight();

                panX = (viewWidth - w) / 2.0;
                panY = (viewHeight - h) / 2.0;
                repaint();
            }
        });

        revalidate();
        repaint();
    }
    private void erasePixel(int x, int y) {
        if (x >= 0 && y >= 0 && x < image.getWidth() && y < image.getHeight()) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.CLEAR)); // Ensure it erases
            g2.setColor(new Color(0, 0, 0, 0)); // Fully transparent
            g2.fillRect(x, y, brushSize, brushSize); // Erase in a small square
            repaint();
        }
    }




    public void setPixelPerfectMode(boolean enabled) {
        this.pixelPerfectMode = enabled;
    }

    //timer
    public void stopPixelCorrection() {
        if (pixelCorrectionTimer != null) {
            pixelCorrectionTimer.stop();
        }
    }
    private void initCanvas() {
        int w = 512;
        int h = 512;
        image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, w, h);

        zoomFactor = 1.0;

        // Center the canvas in the JScrollPane
        if (getParent() instanceof JViewport) {
            JViewport viewport = (JViewport) getParent();
            int viewWidth = viewport.getWidth();
            int viewHeight = viewport.getHeight();

            panX = (viewWidth - w) / 2.0;
            panY = (viewHeight - h) / 2.0;
        }

        revalidate();
        repaint();
    }
    public void centerCanvas() {
        if (getParent() instanceof JViewport) {
            JViewport viewport = (JViewport) getParent();
            int viewWidth = viewport.getWidth();
            int viewHeight = viewport.getHeight();

            panX = (viewWidth - image.getWidth()) / 2.0;
            panY = (viewHeight - image.getHeight()) / 2.0;
            repaint();
        }
    }


    private void resizeCanvas() {
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;
        BufferedImage newImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D gg = newImage.createGraphics();
        gg.setColor(Color.WHITE);
        gg.fillRect(0, 0, w, h);
        gg.drawImage(image, 0, 0, null);
        gg.dispose();
        image = newImage;
        g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER)); // Ensures new colors replace old ones

    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(1000, 700);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        // Fill background with gray
        g2d.setColor(new Color(35, 35, 35));
        g2d.fillRect(0, 0, getWidth(), getHeight());

        // Ensure strokes overwrite previous ones (no transparency issues)
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));

        // Calculate zoomed image size
        int scaledW = (int) (image.getWidth() * zoomFactor);
        int scaledH = (int) (image.getHeight() * zoomFactor);
        // Ensure strokes overwrite previous ones (no transparency issues)
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));
        // Draw the image at the correct position
        g2d.drawImage(image, (int) panX, (int) panY, scaledW, scaledH, null);

        // Draw guide lines for the line tool (if active)
        if ("LINE".equals(currentTool) && drawingLine) {
            int dx = lineEndX - lineStartX;
            int dy = lineEndY - lineStartY;
            double angleDeg = Math.toDegrees(Math.atan2(dy, dx));
            double snappedAngleDeg = Math.round(angleDeg / 10.0) * 10.0;
            double length = Math.hypot(dx, dy);
            double rad = Math.toRadians(snappedAngleDeg);
            int snappedX = lineStartX + (int) Math.round(length * Math.cos(rad));
            int snappedY = lineStartY + (int) Math.round(length * Math.sin(rad));
            // **SHIFT CANVAS DOWN TO AVOID OVERLAP**
            int offsetY = 40; // Push down so top bar is not covered
            g2d.drawImage(image, (int) panX, (int) panY + offsetY, scaledW, scaledH, null);
            int screenX1 = (int) (lineStartX * zoomFactor + panX);
            int screenY1 = (int) (lineStartY * zoomFactor + panY);
            int screenX2 = (int) (snappedX * zoomFactor + panX);
            int screenY2 = (int) (snappedY * zoomFactor + panY);

            g2d.setColor(Color.BLACK);
            g2d.setStroke(new BasicStroke(1));
            g2d.drawLine(screenX1, screenY1, screenX2, screenY2);
        }
    }



    private int screenToCanvasX(int sx) {
        return (int) ((sx - panX) / zoomFactor);
    }
    private int screenToCanvasY(int sy) {
        return (int) ((sy - panY) / zoomFactor);
    }

    // ----- Tool and Brush Methods -----
    public void setBrushMode() {
        currentTool = "BRUSH";
        drawingLine = false;
    }
    public void setEraserMode() {
        currentTool = "ERASER";
        drawingLine = false;
    }
    public void setBucketMode() {
        currentTool = "BUCKET";
        drawingLine = false;
    }
    public void setLineMode() {
        currentTool = "LINE";
    }
    public void setColorPickerMode() {
        currentTool = "COLOR_PICKER";
    }
    public void setBrushColor(Color color) {
        this.currentColor = color;
    }
    public void setBrushSize(int size) {
        this.brushSize = size;
    }

    // ----- Bucket Fill -----
    private void floodFill(int x, int y, Color newColor) {
        if (image == null) return;
        int targetColor = image.getRGB(x, y);
        int fillColor = newColor.getRGB();
        if (targetColor == fillColor) return;
        Queue<Point> queue = new LinkedList<>();
        queue.add(new Point(x, y));
        while (!queue.isEmpty()) {
            Point p = queue.poll();
            int px = p.x, py = p.y;
            if (px < 0 || py < 0 || px >= image.getWidth() || py >= image.getHeight()) continue;
            if (image.getRGB(px, py) != targetColor) continue;
            image.setRGB(px, py, fillColor);
            queue.add(new Point(px + 1, py));
            queue.add(new Point(px - 1, py));
            queue.add(new Point(px, py + 1));
            queue.add(new Point(px, py - 1));
        }
        repaint();
    }

    // ----- Clear / Undo / Redo -----
    public void clearCanvas() {
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, image.getWidth(), image.getHeight());
        repaint();
    }

    private void saveState() {
        BufferedImage snapshot = copyImage(image);
        undoStack.push(snapshot);
        redoStack.clear();
    }

    public void undo() {
        if (!undoStack.isEmpty()) {
            redoStack.push(copyImage(image));
            image = undoStack.pop();
            g2 = image.createGraphics();
            repaint();
        }
    }

    public void redo() {
        if (!redoStack.isEmpty()) {
            undoStack.push(copyImage(image));
            image = redoStack.pop();
            g2 = image.createGraphics();
            repaint();
        }
    }

    private BufferedImage copyImage(BufferedImage img) {
        BufferedImage copy = new BufferedImage(img.getWidth(), img.getHeight(), img.getType());
        Graphics2D gg = copy.createGraphics();
        gg.drawImage(img, 0, 0, null);
        gg.dispose();
        return copy;
    }

    // ----- Zoom & Pan -----
    public void zoom(double factor) {
        zoom(factor, getWidth() / 2, getHeight() / 2);
    }

    public void zoom(double factor, int mouseX, int mouseY) {
        double oldZoom = zoomFactor;
        zoomFactor *= factor;
        if (zoomFactor < 0.1) zoomFactor = 0.1;
        if (zoomFactor > 20.0) zoomFactor = 20.0;
        double relX = (mouseX - panX) / oldZoom;
        double relY = (mouseY - panY) / oldZoom;
        panX = mouseX - (relX * zoomFactor);
        panY = mouseY - (relY * zoomFactor);
        // Reset freehand references after zoom
        prevX = -1;
        prevY = -1;
        revalidate();
        repaint();
    }

    public void resetZoom() {
        zoomFactor = 1.0;
        panX = 0;
        panY = 0;
        revalidate();
        repaint();
    }

    // ----- Save / Open -----
    public void saveImage() {
        JFileChooser fileChooser = new JFileChooser();
        int choice = fileChooser.showSaveDialog(null);
        if (choice == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try {
                ImageIO.write(image, "png", file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void openImage() {
        JFileChooser fileChooser = new JFileChooser();
        int choice = fileChooser.showOpenDialog(null);
        if (choice == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try {
                image = ImageIO.read(file);
                g2 = image.createGraphics();
                repaint();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // ----- Tool State Methods -----
    public void setTool(String tool) {
        if (!isTemporaryToolActive) {
            previousTool = currentTool;
            isTemporaryToolActive = true;
        }
        currentTool = tool;
    }

    public void revertTool() {
        if (isTemporaryToolActive) {
            currentTool = previousTool;
            isTemporaryToolActive = false;
        }
    }

    public Color getBrushColor() {
        return currentColor;
    }

    // ----- Scrollable Interface Methods -----
    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        if (getParent() instanceof JViewport) {
            return getPreferredSize().width < ((JViewport) getParent()).getWidth();
        }
        return false;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        if (getParent() instanceof JViewport) {
            return getPreferredSize().height < ((JViewport) getParent()).getHeight();
        }
        return false;
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        return 10; // Adjust as needed
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        return 50; // Adjust as needed
    }
}
