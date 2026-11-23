package com.heal.doan_ltm.components;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class BubblePanel extends JPanel {
    private Color bg;
    private Color borderColor;

    public BubblePanel(Color bg) {
        this(bg, new Color(220, 226, 235));
    }

    public BubblePanel(Color bg, Color borderColor) {
        this.bg = bg;
        this.borderColor = borderColor;
        setOpaque(false);
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(9, 14, 9, 14));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(bg);
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
        if (borderColor != null) {
            g2.setColor(borderColor);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
        }
        g2.dispose();
        super.paintComponent(g);
    }
}