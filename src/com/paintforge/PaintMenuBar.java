package com.paintforge;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;

public class PaintMenuBar extends JMenuBar {
    private final JFrame parentFrame;
    private final PaintCanvas canvas;
    public PaintMenuBar(PaintCanvas canvas, JFrame parentFrame) {
        this.canvas = canvas;
        this.parentFrame = parentFrame;
        // 🔹 Set Dark Theme for Top Menu Bar
        this.setBackground(new Color(60, 63, 65)); // Dark gray
        this.setBorderPainted(false);

        // 🔹 File Menu
        JMenu fileMenu = createStyledMenu("File");
        fileMenu.setBorder(new LineBorder(new Color(0,0,0,0)));
        //Open new file dialog
        JMenuItem newFile = createStyledMenuItem("New Ctrl+N");
        newFile.addActionListener(e -> {
            // Show the "New Document" dialog instead of just clearing the canvas
            showNewDocumentDialog();
        });
        fileMenu.add(newFile);
        JMenuItem saveFile = createStyledMenuItem("Save Ctrl+S");
        JMenuItem openFile = createStyledMenuItem("Open Ctrl+O");

        newFile.addActionListener(e -> canvas.clearCanvas());
        saveFile.addActionListener(e -> canvas.saveImage());
        openFile.addActionListener(e -> canvas.openImage());

        fileMenu.add(newFile);
        fileMenu.add(saveFile);
        fileMenu.add(openFile);

        // 🔹 Edit Menu
        JMenu editMenu = createStyledMenu("Edit");
        JMenuItem undo = createStyledMenuItem("Undo Ctrl-Z");
        JMenuItem redo = createStyledMenuItem("Redo Ctrl-Y");
        JMenuItem clear = createStyledMenuItem("Clear Canvas Ctrl-E");

        undo.addActionListener(e -> canvas.undo());
        redo.addActionListener(e -> canvas.redo());
        clear.addActionListener(e -> canvas.clearCanvas());

        editMenu.add(undo);
        editMenu.add(redo);
        editMenu.add(clear);

        // 🔹 View Menu
        JMenu viewMenu = createStyledMenu("View");
        JMenuItem zoomIn = createStyledMenuItem("Zoom In");
        JMenuItem zoomOut = createStyledMenuItem("Zoom Out");
        JMenuItem resetZoom = createStyledMenuItem("Reset Zoom");

        zoomIn.addActionListener(e -> canvas.zoom(1.2));  // 🔹 Now zooms in!
        zoomOut.addActionListener(e -> canvas.zoom(0.8)); // 🔹 Now zooms out!
        resetZoom.addActionListener(e -> canvas.resetZoom()); // 🔹 Resets zoom to default

        viewMenu.add(zoomIn);
        viewMenu.add(zoomOut);
        viewMenu.add(resetZoom);

        // Add menus to the menu bar
        this.add(fileMenu);
        this.add(editMenu);
        this.add(viewMenu);
    }
    //Dialog for New File
    private void showNewDocumentDialog() {
        // Create a modal dialog (blocks interaction with the main frame until closed)
        JDialog dialog = new JDialog(parentFrame, "New Document", true);
        dialog.setSize(400, 200);
        dialog.setLocationRelativeTo(parentFrame);
        dialog.setLayout(new BorderLayout(10, 10));

        // Panel for inputs
        JPanel inputPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.gridx = 0; gbc.gridy = 0;
        inputPanel.add(new JLabel("Width:"), gbc);

        gbc.gridx = 1;
        JTextField widthField = new JTextField("512", 8);
        inputPanel.add(widthField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        inputPanel.add(new JLabel("Height:"), gbc);

        gbc.gridx = 1;
        JTextField heightField = new JTextField("512", 8);
        inputPanel.add(heightField, gbc);

        dialog.add(inputPanel, BorderLayout.CENTER);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton createBtn = new JButton("Create");
        JButton cancelBtn = new JButton("Cancel");
        buttonPanel.add(createBtn);
        buttonPanel.add(cancelBtn);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        // Handle "Create"
        createBtn.addActionListener(e -> {
            try {
                int w = Integer.parseInt(widthField.getText());
                int h = Integer.parseInt(heightField.getText());
                if (w <= 0 || h <= 0) {
                    JOptionPane.showMessageDialog(dialog,
                            "Width and height must be positive.",
                            "Invalid Input", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                // Re-initialize the canvas with new dimensions
                canvas.initCanvas(w, h);
                dialog.dispose();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog,
                        "Width and height must be valid integers.",
                        "Invalid Input", JOptionPane.ERROR_MESSAGE);
            }
        });

        // Handle "Cancel"
        cancelBtn.addActionListener(e -> dialog.dispose());

        dialog.setVisible(true);
    }


    private JMenu createStyledMenu(String name) {
        JMenu menu = new JMenu(name);
        menu.setForeground(Color.WHITE);
        menu.setBackground(new Color(60, 63, 65));
        menu.setOpaque(true);
        return menu;
    }

    private JMenuItem createStyledMenuItem(String name) {
        JMenuItem menuItem = new JMenuItem(name);
        menuItem.setForeground(Color.WHITE);
        menuItem.setBackground(new Color(60, 63, 65));
        menuItem.setOpaque(true);
        menuItem.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        return menuItem;
    }
}
