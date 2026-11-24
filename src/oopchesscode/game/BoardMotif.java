package oopchesscode.game;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/** A decorative position painted with the same vectors as the playable board. */
final class BoardMotif extends JComponent {
    private static final long serialVersionUID = 1L;
    private static final String[] POSITION = {
            "....k...", "p.p..pp.", "........", "..n.p...", "....P...", "...B.N..", ".P.P.PP.", ".....K.."
    };

    BoardMotif() {
        setPreferredSize(new Dimension(230, 230));
    }

    @Override protected void paintComponent(Graphics graphics) {
        int side = Math.min(getWidth() - 16, getHeight() - 20);
        if (side < 40) return;
        double size = side / 8.0;
        double x = (getWidth() - side) / 2.0;
        double y = (getHeight() - side) / 2.0;
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        var c = VisualStyle.palette();
        g.setColor(c.boardFrame());
        g.fill(new RoundRectangle2D.Double(x - 7, y - 7, side + 14, side + 14, 15, 15));
        g.clip(new RoundRectangle2D.Double(x, y, side, side, 4, 4));
        for (int r = 0; r < 8; r++) {
            for (int col = 0; col < 8; col++) {
                double left = x + col * size, top = y + r * size;
                boolean light = (r + col) % 2 == 0;
                g.setColor(r == 5 && col == 5 ? c.selection() : light ? c.lightSquare() : c.darkSquare());
                g.fillRect((int) left, (int) top, (int) Math.ceil(size) + 1, (int) Math.ceil(size) + 1);
                char piece = POSITION[r].charAt(col);
                if (piece != '.') PieceArt.paint(g, left + size * .08, top + size * .10, size * .84,
                        piece, Character.isUpperCase(piece));
            }
        }
        g.dispose();
    }
}
