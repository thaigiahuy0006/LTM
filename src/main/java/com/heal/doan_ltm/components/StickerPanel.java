package com.heal.doan_ltm.components;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;

public class StickerPanel extends JPanel {
    public static final String[] STICKER_IDS = {
            "ok", "haha", "love", "cry", "cool", "hello", "sleep", "angry"
    };

    private final String stickerId;
    private final int size;

    public StickerPanel(String stickerId, int size) {
        this.stickerId = stickerId;
        this.size = size;
        setOpaque(false);
        setPreferredSize(new Dimension(size, size));
        setMinimumSize(new Dimension(size, size));
        setMaximumSize(new Dimension(size, size));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        float s = size / 100f;
        int pad = Math.round(6 * s);
        int d = size - pad * 2;

        // Nền tròn màu pastel tươi sáng
        Color bg = new Color(255, 224, 102);
        String caption = "OK nhá!";
        switch (stickerId) {
            case "haha": bg = new Color(255, 212, 59); caption = "HAHA"; break;
            case "love": bg = new Color(255, 135, 135); caption = "YÊU QUÁ"; break;
            case "cry": bg = new Color(116, 192, 252); caption = "HUHU"; break;
            case "cool": bg = new Color(169, 227, 75); caption = "ĐỈNH!"; break;
            case "hello": bg = new Color(102, 217, 232); caption = "HELLO!"; break;
            case "sleep": bg = new Color(177, 151, 252); caption = "Zzz..."; break;
            case "angry": bg = new Color(255, 146, 43); caption = "HỨ!"; break;
        }

        // Bóng đổ nhẹ
        g2.setColor(new Color(0, 0, 0, 22));
        g2.fillOval(pad + 2, pad + 3, d, d);

        // Thân nhân vật sticker
        g2.setColor(bg);
        g2.fillOval(pad, pad, d, d);
        g2.setColor(new Color(45, 52, 64));
        g2.setStroke(new BasicStroke(2.2f * s, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawOval(pad, pad, d, d);

        // Má hồng
        g2.setColor(new Color(255, 107, 107, 130));
        g2.fillOval(Math.round(20 * s), Math.round(48 * s), Math.round(13 * s), Math.round(8 * s));
        g2.fillOval(Math.round(67 * s), Math.round(48 * s), Math.round(13 * s), Math.round(8 * s));

        // Biểu cảm mắt & miệng
        g2.setColor(new Color(35, 40, 50));
        switch (stickerId) {
            case "love":
                g2.setColor(new Color(224, 49, 49));
                drawHeart(g2, 34 * s, 38 * s, 9 * s);
                drawHeart(g2, 66 * s, 38 * s, 9 * s);
                g2.setColor(new Color(35, 40, 50));
                g2.draw(new Arc2D.Float(38 * s, 46 * s, 24 * s, 16 * s, 190, 160, Arc2D.OPEN));
                break;
            case "haha":
                g2.draw(new Arc2D.Float(28 * s, 32 * s, 14 * s, 10 * s, 0, 180, Arc2D.OPEN));
                g2.draw(new Arc2D.Float(58 * s, 32 * s, 14 * s, 10 * s, 0, 180, Arc2D.OPEN));
                g2.setColor(new Color(224, 49, 49));
                g2.fill(new Arc2D.Float(34 * s, 45 * s, 32 * s, 22 * s, 180, 180, Arc2D.CHORD));
                break;
            case "cry":
                g2.draw(new Line2D.Float(28 * s, 38 * s, 41 * s, 38 * s));
                g2.draw(new Line2D.Float(59 * s, 38 * s, 72 * s, 38 * s));
                g2.setColor(new Color(28, 126, 214));
                g2.fillRect(Math.round(32 * s), Math.round(40 * s), Math.round(5 * s), Math.round(24 * s));
                g2.fillRect(Math.round(63 * s), Math.round(40 * s), Math.round(5 * s), Math.round(24 * s));
                g2.setColor(new Color(35, 40, 50));
                g2.draw(new Arc2D.Float(40 * s, 54 * s, 20 * s, 12 * s, 10, 160, Arc2D.OPEN));
                break;
            case "cool":
                g2.fillRoundRect(Math.round(23 * s), Math.round(31 * s), Math.round(23 * s), Math.round(13 * s), 6, 6);
                g2.fillRoundRect(Math.round(54 * s), Math.round(31 * s), Math.round(23 * s), Math.round(13 * s), 6, 6);
                g2.draw(new Line2D.Float(46 * s, 35 * s, 54 * s, 35 * s));
                g2.draw(new Arc2D.Float(38 * s, 46 * s, 24 * s, 14 * s, 200, 140, Arc2D.OPEN));
                break;
            case "sleep":
                g2.draw(new Arc2D.Float(28 * s, 35 * s, 14 * s, 8 * s, 180, 180, Arc2D.OPEN));
                g2.draw(new Arc2D.Float(58 * s, 35 * s, 14 * s, 8 * s, 180, 180, Arc2D.OPEN));
                g2.fill(new Ellipse2D.Float(45 * s, 50 * s, 10 * s, 10 * s));
                break;
            case "angry":
                g2.draw(new Line2D.Float(28 * s, 31 * s, 42 * s, 36 * s));
                g2.draw(new Line2D.Float(72 * s, 31 * s, 58 * s, 36 * s));
                g2.fill(new Ellipse2D.Float(32 * s, 37 * s, 7 * s, 7 * s));
                g2.fill(new Ellipse2D.Float(61 * s, 37 * s, 7 * s, 7 * s));
                g2.draw(new Arc2D.Float(39 * s, 54 * s, 22 * s, 12 * s, 15, 150, Arc2D.OPEN));
                break;
            default: // ok & hello
                g2.fill(new Ellipse2D.Float(31 * s, 34 * s, 7.5f * s, 7.5f * s));
                g2.fill(new Ellipse2D.Float(61.5f * s, 34 * s, 7.5f * s, 7.5f * s));
                g2.draw(new Arc2D.Float(37 * s, 44 * s, 26 * s, 17 * s, 190, 160, Arc2D.OPEN));
                break;
        }

        // Nhãn chữ trắng viền xanh chuẩn sticker Zalo ở dưới cùng
        Font f = new Font("SansSerif", Font.BOLD, Math.max(9, Math.round(11 * s)));
        g2.setFont(f);
        FontMetrics fm = g2.getFontMetrics();
        int tw = fm.stringWidth(caption);
        int tx = (size - tw) / 2;
        int ty = Math.round(86 * s);

        g2.setColor(new Color(0, 104, 255));
        g2.fillRoundRect(tx - 6, ty - fm.getAscent() - 1, tw + 12, fm.getHeight() + 2, 10, 10);
        g2.setColor(Color.WHITE);
        g2.drawString(caption, tx, ty);

        g2.dispose();
    }

    private void drawHeart(Graphics2D g2, float cx, float cy, float r) {
        Path2D h = new Path2D.Float();
        h.moveTo(cx, cy + r * 0.7f);
        h.curveTo(cx - r * 1.4f, cy - r * 0.3f, cx - r * 0.6f, cy - r * 1.2f, cx, cy - r * 0.4f);
        h.curveTo(cx + r * 0.6f, cy - r * 1.2f, cx + r * 1.4f, cy - r * 0.3f, cx, cy + r * 0.7f);
        g2.fill(h);
    }
}