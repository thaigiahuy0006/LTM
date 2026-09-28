package com.heal.doan_ltm.components;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class BubblePanel extends JPanel {
    private Color bg;

    public BubblePanel(Color bg) {
        this.bg = bg;
        setOpaque(false);
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(10, 15, 10, 15));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(bg);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 22, 22);
        g2.dispose();
        super.paintComponent(g);
    }
}