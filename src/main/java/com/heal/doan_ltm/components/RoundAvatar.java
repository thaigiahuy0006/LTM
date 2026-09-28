package com.heal.doan_ltm.components;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;

public class RoundAvatar extends JComponent {
    private Image img;
    private String avatarText;

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

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int d = Math.min(getWidth(), getHeight());

        if (img != null) {
            g2.setClip(new Ellipse2D.Float(0, 0, d, d));
            int iw = img.getWidth(null), ih = img.getHeight(null);
            double scale = Math.max((double) d / iw, (double) d / ih);
            int sw = (int) (iw * scale), sh = (int) (ih * scale);
            int sx = (d - sw) / 2, sy = (d - sh) / 2;
            g2.drawImage(img, sx, sy, sw, sh, null);
        } else {
            // Vẽ nền tròn
            g2.setColor(getBackground());
            g2.fillOval(0, 0, d, d);

            // Ép chữ luôn nằm đúng giữa tâm vòng tròn
            if (avatarText != null && !avatarText.isEmpty()) {
                g2.setColor(getForeground());
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int x = (d - fm.stringWidth(avatarText)) / 2;
                int y = ((d - fm.getHeight()) / 2) + fm.getAscent();
                g2.drawString(avatarText, x, y);
            }
        }
        g2.dispose();
    }
}