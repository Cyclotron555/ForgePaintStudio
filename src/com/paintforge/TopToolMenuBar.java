package com.paintforge;

import javax.swing.*;
import java.awt.*;

public class TopToolMenuBar extends JToolBar {
    private JCheckBox pixelPerfectCheckbox;

    public TopToolMenuBar(PaintCanvas canvas) {
        setFloatable(false); // Prevents it from floating/moving
        setOpaque(true);
        setBackground(new Color(45, 45, 45));

        // Ensure it has a defined height
        setPreferredSize(new Dimension(800, 40));

        // Pixel Perfect Checkbox
        pixelPerfectCheckbox = new JCheckBox("Pixel Perfect");
        pixelPerfectCheckbox.setForeground(Color.WHITE);
        pixelPerfectCheckbox.setOpaque(false);
        pixelPerfectCheckbox.setSelected(true);
        pixelPerfectCheckbox.addActionListener(e -> canvas.setPixelPerfectMode(pixelPerfectCheckbox.isSelected()));

        add(pixelPerfectCheckbox);
    }
}
