package com.heal.doan_ltm.components;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class FlatButton extends JButton {
    private Color normalBg, hoverBg, pressedBg;
    private int radius;

    public FlatButton(String text, Color bg, Color fg, int radius) {
        super(text);
        this.normalBg = bg;
        this.radius = radius;
        if (bg.getAlpha() == 0) {
            this.hoverBg = new Color(0, 0, 0, 15);
            this.pressedBg = new Color(0, 0, 0, 30);
        } else {
            this.hoverBg = brighten(bg, 0.1f);
            this.pressedBg = darken(bg, 0.1f);
        }
        setForeground(fg);
        setFont(new Font("SansSerif", Font.BOLD, 14));
        setFocusPainted(false); setContentAreaFilled(false); setBorderPainted(false);
        setMargin(new Insets(0, 0, 0, 0));
        setCursor(new Cursor(Cursor.HAND_CURSOR));

        addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { if(isEnabled()) repaint(); }
            public void mouseExited(MouseEvent e) { if(isEnabled()) repaint(); }
            public void mousePressed(MouseEvent e) { if(isEnabled()) repaint(); }
            public void mouseReleased(MouseEvent e) { if(isEnabled()) repaint(); }
        });
    }

    private Color brighten(Color c, float f) {
        if (c.equals(Color.WHITE)) return new Color(240, 240, 240);
        int r = Math.min(255, (int)(c.getRed() + (255 - c.getRed()) * f));
        int g = Math.min(255, (int)(c.getGreen() + (255 - c.getGreen()) * f));
        int b = Math.min(255, (int)(c.getBlue() + (255 - c.getBlue()) * f));
        return new Color(r, g, b);
    }

    private Color darken(Color c, float f) {
        int r = Math.max(0, (int)(c.getRed() * (1 - f)));
        int g = Math.max(0, (int)(c.getGreen() * (1 - f)));
        int b = Math.max(0, (int)(c.getBlue() * (1 - f)));
        return new Color(r, g, b);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (!isEnabled()) g2.setColor(new Color(230, 230, 230));
        else if (getModel().isPressed()) g2.setColor(pressedBg);
        else if (getModel().isRollover()) g2.setColor(hoverBg);
        else g2.setColor(normalBg);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
        g2.dispose();
        super.paintComponent(g);
    }
}