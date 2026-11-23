package com.heal.doan_ltm.components;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;

public class RoundAvatar extends JComponent {
    private Image img;
    private String avatarText;
    private boolean showOnlineDot = false;
    private boolean isOnline = false;

    public RoundAvatar(String text) {
        this.avatarText = text;
        setOpaque(false);
    }

    public void setImage(Image img) {
        this.img = img;
        repaint();
    }

    public void setAvatarText(String text) {
        this.avatarText = text;
        repaint();
    }

    public void setOnlineStatus(boolean showDot, boolean online) {
        this.showOnlineDot = showDot;
        this.isOnline = online;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int d = Math.min(getWidth(), getHeight());

        if (img != null) {
            Shape oldClip = g2.getClip();
            g2.setClip(new Ellipse2D.Float(0, 0, d, d));
            int iw = img.getWidth(null), ih = img.getHeight(null);
            double scale = Math.max((double) d / iw, (double) d / ih);
            int sw = (int) (iw * scale), sh = (int) (ih * scale);
            int sx = (d - sw) / 2, sy = (d - sh) / 2;
            g2.drawImage(img, sx, sy, sw, sh, null);
            g2.setClip(oldClip);
        } else {
            g2.setColor(getBackground());
            g2.fillOval(0, 0, d, d);

            if (avatarText != null && !avatarText.isEmpty()) {
                g2.setColor(getForeground());
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int x = (d - fm.stringWidth(avatarText)) / 2;
                int y = ((d - fm.getHeight()) / 2) + fm.getAscent();
                g2.drawString(avatarText, x, y);
            }
        }

        // Chấm tròn xanh báo Online góc dưới bên phải giống Zalo
        if (showOnlineDot && isOnline) {
            int dotSize = Math.max(10, d / 4);
            int dx = d - dotSize;
            int dy = d - dotSize;
            g2.setColor(Color.WHITE);
            g2.fillOval(dx - 2, dy - 2, dotSize + 4, dotSize + 4);
            g2.setColor(new Color(46, 204, 113));
            g2.fillOval(dx, dy, dotSize, dotSize);
        }

        g2.dispose();
    }
}