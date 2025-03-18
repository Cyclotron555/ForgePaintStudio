package com.paintforge;

import com.formdev.flatlaf.FlatDarculaLaf;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.net.URL;

public class PaintForgeApp {
    public static void main(String[] args) {
        // Apply FlatLaf theme
        try {
            UIManager.setLookAndFeel(new FlatDarculaLaf());
        } catch (UnsupportedLookAndFeelException ex) {
            ex.printStackTrace();
        }

        JFrame frame = new JFrame("PaintForge Studio");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        // Create the canvas
        PaintCanvas canvas = new PaintCanvas();
        JScrollPane scrollPane = new JScrollPane(canvas);
        scrollPane.setBorder(null); // Remove unwanted borders
        scrollPane.setPreferredSize(new Dimension(800, 600)); // Set a default size
        scrollPane.getViewport().setBackground(new Color(35, 35, 35)); // Match theme
        scrollPane.getViewport().setOpaque(true); // Ensure it's visible


        // Create the top tool menu
        TopToolMenuBar topToolMenu = new TopToolMenuBar(canvas);

        // Create a panel to contain the top menu bar
        // Ensure the toolbar is inside a JPanel to prevent overlap issues
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(topToolMenu, BorderLayout.NORTH);
        topPanel.setPreferredSize(new Dimension(frame.getWidth(), 40));
        topPanel.setOpaque(true);
        topPanel.setBackground(new Color(45, 45, 45));

// Add the top panel FIRST so it remains on top
        frame.add(topPanel, BorderLayout.NORTH);
        frame.add(scrollPane, BorderLayout.CENTER);


        // Add the panel to the frame
        frame.add(topPanel, BorderLayout.NORTH);

        // Add remaining UI elements
        frame.add(scrollPane, BorderLayout.CENTER);

        // Set up frame properties AFTER adding everything
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        // Ensure repainting updates properly
        SwingUtilities.invokeLater(() -> {
            canvas.centerCanvas();
            frame.revalidate();
            frame.repaint();
        });

        //URL mainIconUrl = PaintForgeApp.class.getResource("/com/paintforge/resources/icons/hammer.png");
        //if (mainIconUrl != null) {
        //    ImageIcon arrowIcon = new ImageIcon(mainIconUrl);
        //    frame.setIconImage(arrowIcon.getImage());
        //} else {
        //    JOptionPane.showMessageDialog(frame, "Icon image not found.");
        //}




        //Alt push to temporary get the colorpicker eyedropper
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(event -> {
            if (event.getKeyCode() == KeyEvent.VK_ALT) {
                if (event.getID() == KeyEvent.KEY_PRESSED) {
                    System.out.println("Alt PRESSED -> switching to COLOR_PICKER");
                    canvas.setTool("COLOR_PICKER");
                } else if (event.getID() == KeyEvent.KEY_RELEASED) {
                    System.out.println("Alt RELEASED -> reverting tool");
                    canvas.revertTool(); // Restore previous tool
                }
                return true;
            }
            return false;
        });


        // Setup UI
        frame.setJMenuBar(new PaintMenuBar(canvas, frame));

        // Left toolbar
        JPanel toolbarPanel = new JPanel();
        toolbarPanel.setPreferredSize(new Dimension(32, 0));
        //toolbarPanel.setBackground(new Color(60, 63, 65));
        toolbarPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 6));

        JButton brushBtn = createIconButton("brush.png", "Brush (Shortcut: B)");
        brushBtn.addActionListener(e -> canvas.setBrushMode());

        JButton colorPickerBtn = createIconButton("eyedropper.png", "Pick Color (Shortcut: I)");
        colorPickerBtn.addActionListener(e -> canvas.setColorPickerMode());

        JButton eraserBtn = createIconButton("eraser.png", "Eraser (Shortcut: E)");
        eraserBtn.addActionListener(e -> canvas.setEraserMode());

        JButton bucketBtn = createIconButton("paint-bucket.png", "Fill (Shortcut: G)");
        bucketBtn.addActionListener(e -> canvas.setBucketMode());

        JButton clearBtn = createIconButton("clear.png", "Clear (Shortcut: Ctrl+E)");
        clearBtn.addActionListener(e -> canvas.clearCanvas());

        JButton lineBtn = createIconButton("line.png", "Line (Shortcut: L)");
        lineBtn.addActionListener(e -> canvas.setLineMode());

        JButton[] buttons = {
                brushBtn, colorPickerBtn, eraserBtn, bucketBtn, clearBtn, lineBtn
        };
        for (JButton btn : buttons) {
            btn.setPreferredSize(new Dimension(24, 24));
            toolbarPanel.add(btn);
        }

        // Right properties panel
        JPanel propertiesPanel = new JPanel();
        propertiesPanel.setPreferredSize(new Dimension(220, 0));
        propertiesPanel.setMinimumSize(new Dimension(220, 0));
        propertiesPanel.setBackground(new Color(60, 63, 65));
        propertiesPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 10));

        // Enhanced color wheel
        ColorWheel colorWheel = new ColorWheel(100);
        propertiesPanel.add(colorWheel);

        // Brightness slider
        JSlider brightnessSlider = new JSlider(JSlider.HORIZONTAL, 0, 100, 100);
        brightnessSlider.setPreferredSize(new Dimension(200, 48));
        brightnessSlider.setMajorTickSpacing(25);
        brightnessSlider.setPaintTicks(true);
        brightnessSlider.setPaintLabels(true);
        brightnessSlider.addChangeListener(e -> {
            float b = brightnessSlider.getValue() / 100f;
            colorWheel.setBrightness(b);
        });
        propertiesPanel.add(brightnessSlider);

        // Listen for color changes from the wheel and update brush color
        colorWheel.addPropertyChangeListener("selectedColor", evt -> {
            Color newColor = (Color) evt.getNewValue();
            canvas.setBrushColor(newColor);
        });

        // Brush size slider
        JSlider brushSizeSlider = new JSlider(JSlider.HORIZONTAL, 1, 50, 3);
        brushSizeSlider.setPreferredSize(new Dimension(200, 48));
        brushSizeSlider.setMajorTickSpacing(10);
        brushSizeSlider.setMinorTickSpacing(1);
        brushSizeSlider.setPaintTicks(true);
        brushSizeSlider.setPaintLabels(true);
        brushSizeSlider.addChangeListener(e -> canvas.setBrushSize(brushSizeSlider.getValue()));
        propertiesPanel.add(brushSizeSlider);

        // Layout
        // Layout Setup - Set BorderLayout
        frame.setLayout(new BorderLayout());

// 🛠 First, add the TopToolMenuBar so it's drawn first
        frame.add(topPanel, BorderLayout.NORTH);

// Then, add side panels
        frame.add(toolbarPanel, BorderLayout.WEST);
        frame.add(propertiesPanel, BorderLayout.EAST);

// Add the ScrollPane containing the PaintCanvas
        frame.add(scrollPane, BorderLayout.CENTER);

// Make sure everything updates
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

// Ensure the canvas centers properly
        SwingUtilities.invokeLater(() -> {
            canvas.centerCanvas();
            frame.revalidate();  // Ensures layout updates
            frame.repaint();
        });

        // Center the canvas after everything is initialized
        SwingUtilities.invokeLater(() -> {
            canvas.centerCanvas();
        });
        // 🟢 Keyboard Shortcuts
        InputMap inputMap = frame.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = frame.getRootPane().getActionMap();

        inputMap.put(KeyStroke.getKeyStroke("control Z"), "undo");
        actionMap.put("undo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                canvas.undo();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke("control Y"), "redo");
        actionMap.put("redo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                canvas.redo();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke('b'), "brushTool");
        actionMap.put("brushTool", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                canvas.setBrushMode();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke('e'), "eraserTool");
        actionMap.put("eraserTool", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                canvas.setEraserMode();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke('g'), "bucketTool");
        actionMap.put("bucketTool", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                canvas.setBucketMode();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke('l'), "lineTool");
        actionMap.put("lineTool", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                canvas.setLineMode();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke("control E"), "clearCanvas");
        actionMap.put("clearCanvas", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                canvas.clearCanvas();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke('i'), "colorPickerTool");
        actionMap.put("colorPickerTool", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                canvas.setColorPickerMode();
            }
        });

        // Show the frame
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private static JButton createIconButton(String iconName, String fallbackText) {
        URL iconUrl = PaintForgeApp.class.getResource("/com/paintforge/resources/icons/" + iconName);
        JButton btn = new JButton();
        btn.setToolTipText(fallbackText);
        if (iconUrl != null) {
            ImageIcon originalIcon = new ImageIcon(iconUrl);
            Image resizedImg = originalIcon.getImage().getScaledInstance(16, 16, Image.SCALE_SMOOTH);
            btn.setIcon(new ImageIcon(resizedImg));
        } else {
            btn.setForeground(new Color(200, 200, 200));
        }

        btn.setBackground(new Color(60, 63, 65));
        btn.setFocusPainted(false);
        btn.setToolTipText(fallbackText);
        btn.setBorderPainted(false);
        return btn;
    }


}
