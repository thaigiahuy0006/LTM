package com.heal.doan_ltm.components;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;

public class ZaloIcon implements Icon {
    public enum Type {
        SEARCH, ADD_FRIEND, ADD_GROUP,
        STICKER, IMAGE, FILE, MIC,
        PHONE, VIDEO, SEND,
        NAV_CHAT, NAV_GROUP, NAV_BELL, NAV_LAN, NAV_LOGOUT,
        USERS, MORE, SIDEBAR_RIGHT, PIN, CLOCK, LINK
    }

    private final Type type;
    private final int size;
    private final Color color;

    public ZaloIcon(Type type, int size, Color color) {
        this.type = type;
        this.size = size;
        this.color = color;
    }

    @Override
    public int getIconWidth() { return size; }

    @Override
    public int getIconHeight() { return size; }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2.translate(x, y);
        g2.setColor(color);

        float s = size / 24f;
        g2.setStroke(new BasicStroke(1.75f * s, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        switch (type) {
            case SEARCH:
                g2.draw(new Ellipse2D.Float(3.5f * s, 3.5f * s, 12.5f * s, 12.5f * s));
                g2.draw(new Line2D.Float(14.5f * s, 14.5f * s, 20.5f * s, 20.5f * s));
                break;

            case ADD_FRIEND:
                g2.draw(new Ellipse2D.Float(5.5f * s, 4f * s, 7f * s, 7f * s));
                g2.draw(new Arc2D.Float(2.5f * s, 13.5f * s, 13f * s, 9f * s, 0, 180, Arc2D.OPEN));
                g2.draw(new Line2D.Float(18.5f * s, 6.5f * s, 18.5f * s, 11.5f * s));
                g2.draw(new Line2D.Float(16f * s, 9f * s, 21f * s, 9f * s));
                break;

            case ADD_GROUP:
                g2.draw(new Ellipse2D.Float(4.5f * s, 5f * s, 6f * s, 6f * s));
                g2.draw(new Arc2D.Float(2f * s, 13.5f * s, 11f * s, 8f * s, 0, 180, Arc2D.OPEN));
                g2.draw(new Arc2D.Float(10f * s, 5.5f * s, 5f * s, 5f * s, -60, 150, Arc2D.OPEN));
                g2.draw(new Arc2D.Float(11f * s, 14f * s, 7f * s, 6.5f * s, 15, 110, Arc2D.OPEN));
                g2.draw(new Line2D.Float(19f * s, 5f * s, 19f * s, 9.5f * s));
                g2.draw(new Line2D.Float(16.75f * s, 7.25f * s, 21.25f * s, 7.25f * s));
                break;

            case STICKER:
                g2.draw(new RoundRectangle2D.Float(3.5f * s, 3.5f * s, 17f * s, 17f * s, 6f * s, 6f * s));
                g2.fill(new Ellipse2D.Float(8.2f * s, 8.8f * s, 2f * s, 2f * s));
                g2.fill(new Ellipse2D.Float(13.8f * s, 8.8f * s, 2f * s, 2f * s));
                g2.draw(new Arc2D.Float(8.2f * s, 10.5f * s, 7.6f * s, 5.5f * s, 195, 150, Arc2D.OPEN));
                break;

            case IMAGE:
                g2.draw(new RoundRectangle2D.Float(3.5f * s, 4.5f * s, 17f * s, 15f * s, 4f * s, 4f * s));
                g2.fill(new Ellipse2D.Float(7.5f * s, 7.8f * s, 2.4f * s, 2.4f * s));
                Path2D mountain = new Path2D.Float();
                mountain.moveTo(4.5f * s, 17.5f * s);
                mountain.lineTo(9.5f * s, 12.5f * s);
                mountain.lineTo(13f * s, 15.8f * s);
                mountain.lineTo(16f * s, 13.2f * s);
                mountain.lineTo(19.5f * s, 17.5f * s);
                g2.draw(mountain);
                break;

            case FILE:
                Path2D clip = new Path2D.Float();
                clip.moveTo(18.5f * s, 11.2f * s);
                clip.lineTo(10.8f * s, 18.9f * s);
                clip.curveTo(8.8f * s, 20.9f * s, 5.6f * s, 20.9f * s, 3.6f * s, 18.9f * s);
                clip.curveTo(1.6f * s, 16.9f * s, 1.6f * s, 13.7f * s, 3.6f * s, 11.7f * s);
                clip.lineTo(12.2f * s, 3.1f * s);
                clip.curveTo(13.6f * s, 1.7f * s, 15.8f * s, 1.7f * s, 17.2f * s, 3.1f * s);
                clip.curveTo(18.6f * s, 4.5f * s, 18.6f * s, 6.7f * s, 17.2f * s, 8.1f * s);
                clip.lineTo(8.8f * s, 16.5f * s);
                clip.curveTo(8.1f * s, 17.2f * s, 7.0f * s, 17.2f * s, 6.3f * s, 16.5f * s);
                clip.curveTo(5.6f * s, 15.8f * s, 5.6f * s, 14.7f * s, 6.3f * s, 14.0f * s);
                clip.lineTo(13.8f * s, 6.5f * s);
                g2.draw(clip);
                break;

            case MIC:
                g2.draw(new RoundRectangle2D.Float(9f * s, 3f * s, 6f * s, 11f * s, 6f * s, 6f * s));
                g2.draw(new Arc2D.Float(5.5f * s, 8.5f * s, 13f * s, 9.5f * s, 180, 180, Arc2D.OPEN));
                g2.draw(new Line2D.Float(12f * s, 18f * s, 12f * s, 21f * s));
                g2.draw(new Line2D.Float(8.5f * s, 21f * s, 15.5f * s, 21f * s));
                break;

            case PHONE:
                Path2D phone = new Path2D.Float();
                phone.moveTo(6.5f * s, 4.5f * s);
                phone.lineTo(9.5f * s, 4.5f * s);
                phone.lineTo(11f * s, 8.5f * s);
                phone.lineTo(8.8f * s, 10.2f * s);
                phone.quadTo(10.5f * s, 13.8f * s, 13.8f * s, 15.2f * s);
                phone.lineTo(15.5f * s, 13f * s);
                phone.lineTo(19.5f * s, 14.5f * s);
                phone.lineTo(19.5f * s, 17.5f * s);
                phone.quadTo(19.5f * s, 20f * s, 16.5f * s, 19.8f * s);
                phone.quadTo(7f * s, 18.5f * s, 4.2f * s, 7.5f * s);
                phone.quadTo(4f * s, 4.5f * s, 6.5f * s, 4.5f * s);
                g2.draw(phone);
                break;

            case VIDEO:
                g2.draw(new RoundRectangle2D.Float(3f * s, 6.5f * s, 12.5f * s, 11f * s, 3.5f * s, 3.5f * s));
                Path2D cam = new Path2D.Float();
                cam.moveTo(15.5f * s, 10.2f * s);
                cam.lineTo(20.8f * s, 7.5f * s);
                cam.lineTo(20.8f * s, 16.5f * s);
                cam.lineTo(15.5f * s, 13.8f * s);
                cam.closePath();
                g2.draw(cam);
                break;

            case SEND:
                Path2D send = new Path2D.Float();
                send.moveTo(3.5f * s, 4.5f * s);
                send.lineTo(20.5f * s, 12f * s);
                send.lineTo(3.5f * s, 19.5f * s);
                send.lineTo(6.5f * s, 12f * s);
                send.closePath();
                g2.fill(send);
                break;

            case NAV_CHAT:
                g2.draw(new RoundRectangle2D.Float(3.5f * s, 4.5f * s, 17f * s, 13.5f * s, 6f * s, 6f * s));
                g2.fill(new Ellipse2D.Float(8.2f * s, 10.5f * s, 1.8f * s, 1.8f * s));
                g2.fill(new Ellipse2D.Float(11.2f * s, 10.5f * s, 1.8f * s, 1.8f * s));
                g2.fill(new Ellipse2D.Float(14.2f * s, 10.5f * s, 1.8f * s, 1.8f * s));
                break;

            case NAV_GROUP:
            case USERS:
                g2.draw(new Ellipse2D.Float(6f * s, 5.5f * s, 5.5f * s, 5.5f * s));
                g2.draw(new Arc2D.Float(3f * s, 13.5f * s, 11.5f * s, 7.5f * s, 0, 180, Arc2D.OPEN));
                g2.draw(new Ellipse2D.Float(13.5f * s, 6.5f * s, 4.5f * s, 4.5f * s));
                g2.draw(new Arc2D.Float(13.5f * s, 14f * s, 7.5f * s, 6.5f * s, 0, 150, Arc2D.OPEN));
                break;

            case NAV_BELL:
                Path2D bell = new Path2D.Float();
                bell.moveTo(6f * s, 16f * s);
                bell.lineTo(6f * s, 10.5f * s);
                bell.curveTo(6f * s, 6.5f * s, 18f * s, 6.5f * s, 18f * s, 10.5f * s);
                bell.lineTo(18f * s, 16f * s);
                bell.lineTo(20f * s, 17.5f * s);
                bell.lineTo(4f * s, 17.5f * s);
                bell.closePath();
                g2.draw(bell);
                g2.draw(new Arc2D.Float(10f * s, 17.5f * s, 4f * s, 3f * s, 180, 180, Arc2D.OPEN));
                break;

            case NAV_LAN:
                g2.draw(new Ellipse2D.Float(3.5f * s, 3.5f * s, 17f * s, 17f * s));
                g2.draw(new Ellipse2D.Float(8.2f * s, 3.5f * s, 7.6f * s, 17f * s));
                g2.draw(new Line2D.Float(3.8f * s, 12f * s, 20.2f * s, 12f * s));
                break;

            case NAV_LOGOUT:
                Path2D door = new Path2D.Float();
                door.moveTo(11f * s, 4.5f * s);
                door.lineTo(5.5f * s, 4.5f * s);
                door.lineTo(5.5f * s, 19.5f * s);
                door.lineTo(11f * s, 19.5f * s);
                g2.draw(door);
                g2.draw(new Line2D.Float(10f * s, 12f * s, 19.5f * s, 12f * s));
                g2.draw(new Line2D.Float(16.2f * s, 8.8f * s, 19.5f * s, 12f * s));
                g2.draw(new Line2D.Float(16.2f * s, 15.2f * s, 19.5f * s, 12f * s));
                break;

            case MORE:
                g2.fill(new Ellipse2D.Float(10.8f * s, 5f * s, 2.4f * s, 2.4f * s));
                g2.fill(new Ellipse2D.Float(10.8f * s, 10.8f * s, 2.4f * s, 2.4f * s));
                g2.fill(new Ellipse2D.Float(10.8f * s, 16.6f * s, 2.4f * s, 2.4f * s));
                break;

            case SIDEBAR_RIGHT:
                g2.draw(new RoundRectangle2D.Float(3.5f * s, 4.5f * s, 17f * s, 15f * s, 3.5f * s, 3.5f * s));
                g2.draw(new Line2D.Float(14.5f * s, 4.5f * s, 14.5f * s, 19.5f * s));
                break;

            case PIN:
                g2.draw(new Line2D.Float(8f * s, 5f * s, 16f * s, 5f * s));
                g2.draw(new Line2D.Float(9.5f * s, 5f * s, 8.5f * s, 12f * s));
                g2.draw(new Line2D.Float(14.5f * s, 5f * s, 15.5f * s, 12f * s));
                g2.draw(new Line2D.Float(6.5f * s, 12f * s, 17.5f * s, 12f * s));
                g2.draw(new Line2D.Float(12f * s, 12f * s, 12f * s, 19.5f * s));
                break;

            case CLOCK:
                g2.draw(new Ellipse2D.Float(3.5f * s, 3.5f * s, 17f * s, 17f * s));
                g2.draw(new Line2D.Float(12f * s, 7.5f * s, 12f * s, 12f * s));
                g2.draw(new Line2D.Float(12f * s, 12f * s, 15.5f * s, 14f * s));
                break;

            case LINK:
                g2.draw(new Arc2D.Float(4f * s, 8f * s, 8f * s, 8f * s, 90, 180, Arc2D.OPEN));
                g2.draw(new Arc2D.Float(12f * s, 8f * s, 8f * s, 8f * s, -90, 180, Arc2D.OPEN));
                g2.draw(new Line2D.Float(8.5f * s, 12f * s, 15.5f * s, 12f * s));
                break;
        }
        g2.dispose();
    }
}