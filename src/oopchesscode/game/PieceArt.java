package oopchesscode.game;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

/** Original, resolution-independent piece silhouettes. */
final class PieceArt {
    private static final Shape FOOT = new RoundRectangle2D.Double(12, 46, 40, 9, 5, 5);

    private PieceArt() {}

    static void paint(Graphics2D graphics, double x, double y, double size, char type, boolean white) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.translate(x, y);
        g.scale(size / 64, size / 64);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Color fill = white ? new Color(0xfff8de) : new Color(0x183129);
        Color edge = white ? new Color(0x33473c) : new Color(0xe5e8d8);
        Color detail = white ? new Color(0x7c8271) : new Color(0xa7b7a8);
        g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        switch (Character.toUpperCase(type)) {
            case 'P' -> {
                mark(g, path(20,46, 26,37, 28,30, 36,30, 38,37, 44,46), fill, edge);
                mark(g, new Ellipse2D.Double(23,14,18,18), fill, edge);
            }
            case 'R' -> {
                mark(g, path(17,46, 22,36, 22,25, 16,22, 16,11, 23,11, 23,17, 28,17, 28,11,
                        36,11, 36,17, 41,17, 41,11, 48,11, 48,22, 42,25, 42,36, 47,46), fill, edge);
                line(g, detail, 23,26, 41,26);
            }
            case 'N' -> {
                Path2D knight = new Path2D.Double();
                knight.moveTo(17,46); knight.quadTo(20,34,33,29);
                knight.lineTo(27,27); knight.lineTo(19,33); knight.lineTo(12,28); knight.lineTo(14,23);
                knight.lineTo(25,14); knight.lineTo(26,8); knight.lineTo(34,13);
                knight.quadTo(50,15,49,34); knight.lineTo(47,46); knight.closePath();
                mark(g, knight, fill, edge);
                g.setColor(detail); g.fill(new Ellipse2D.Double(25,18,4,4));
                line(g, detail, 37,20, 42,27, 42,39);
            }
            case 'B' -> {
                mark(g, path(19,46, 26,35, 38,35, 45,46), fill, edge);
                Path2D miter = new Path2D.Double();
                miter.moveTo(32,11); miter.curveTo(53,26,47,37,32,37);
                miter.curveTo(17,37,11,26,32,11); miter.closePath();
                mark(g, miter, fill, edge);
                mark(g, new Ellipse2D.Double(29,7,6,6), fill, edge);
                line(g, detail, 35,18, 29,28);
            }
            case 'Q' -> {
                mark(g, path(19,46, 16,22, 26,32, 32,15, 38,32, 48,22, 45,46), fill, edge);
                mark(g, new Ellipse2D.Double(12,15,8,8), fill, edge);
                mark(g, new Ellipse2D.Double(28,8,8,8), fill, edge);
                mark(g, new Ellipse2D.Double(44,15,8,8), fill, edge);
                line(g, detail, 21,41, 43,41);
            }
            case 'K' -> {
                mark(g, path(29,25, 29,13, 23,13, 23,8, 29,8, 29,3, 35,3, 35,8, 41,8, 41,13,
                        35,13, 35,25), fill, edge);
                Path2D crown = new Path2D.Double();
                crown.moveTo(19,46); crown.lineTo(21,38);
                crown.curveTo(5,21,22,14,32,25); crown.curveTo(42,14,59,21,43,38);
                crown.lineTo(45,46); crown.closePath();
                mark(g, crown, fill, edge);
                line(g, detail, 23,39, 41,39);
            }
            default -> { g.dispose(); return; }
        }
        mark(g, FOOT, fill, edge);
        g.dispose();
    }

    private static Path2D path(int... points) {
        Path2D path = new Path2D.Double();
        path.moveTo(points[0], points[1]);
        for (int i = 2; i < points.length; i += 2) path.lineTo(points[i], points[i + 1]);
        path.closePath();
        return path;
    }

    private static void mark(Graphics2D g, Shape shape, Color fill, Color edge) {
        g.setColor(fill);
        g.fill(shape);
        g.setColor(edge);
        g.draw(shape);
    }

    private static void line(Graphics2D g, Color color, int... points) {
        Path2D line = new Path2D.Double();
        line.moveTo(points[0], points[1]);
        for (int i = 2; i < points.length; i += 2) line.lineTo(points[i], points[i + 1]);
        g.setColor(color);
        g.draw(line);
    }
}
