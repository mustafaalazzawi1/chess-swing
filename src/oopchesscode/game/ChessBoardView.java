package oopchesscode.game;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import static oopchesscode.game.VisualStyle.Role.*;
import static oopchesscode.game.SwingControls.*;

import oopchesscode.game.ChessMatch.Move;
import oopchesscode.pieces.Piece;
import oopchesscode.position.Position;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/** Renders a supplied position and emits square selections; never applies moves. */
final class ChessBoardView {
    private static final Color DARK_MARK = new Color(0x142923);
    private static final Color CHECK = new Color(0xEEA18B);
    private static final int BOARD_INSET = 8;
    private final Square[][] squares = new Square[8][8];
    private final Consumer<Position> onSquare;
    private final JPanel panel;
    private Square boardFocus;
    private boolean flipped;

    ChessBoardView(Consumer<Position> onSquare) {
        this.onSquare = onSquare;
        panel = build();
    }
    JPanel panel() { return panel; }
    void focusDefault(boolean reverse) { focusSquare(squares[reverse ? 1 : 6][reverse ? 3 : 4]); }
    void render(ChessMatch shown, Position selected, List<Move> legal, Move suggestedMove, boolean flipped) {
        this.flipped = flipped;
        List<Move> played = shown.history();
        Move last = played.isEmpty() ? null : played.getLast();
        Position checked = shown.checkedKing();
        var palette = VisualStyle.palette();
        for (Square[] row : squares) {
            for (Square square : row) {
                Position pos = at(square.row, square.col);
                square.piece = shown.pieceAt(pos);
                square.pos = pos;
                square.legal = legal.stream().anyMatch(move -> move.to().equals(pos));
                boolean light = (square.row + square.col) % 2 == 0;
                square.tile = light ? palette.lightSquare() : palette.darkSquare();
                square.last = last != null && (pos.equals(last.from()) || pos.equals(last.to()));
                square.checked = pos.equals(checked);
                square.selected = pos.equals(selected);
                square.suggested = suggestedMove != null
                        && (pos.equals(suggestedMove.from()) || pos.equals(suggestedMove.to()));
                if (square.last) square.tile = light ? palette.lastLight() : palette.lastDark();
                if (square.checked) square.tile = CHECK;
                if (square.selected) square.tile = palette.selection();
                square.getAccessibleContext().setAccessibleName(pos + ", " + description(square.piece)
                        + (square.selected ? ", selected" : "") + (square.checked ? ", in check" : "")
                        + (square.legal ? ", legal destination" : "") + (square.suggested ? ", engine suggestion" : ""));
                square.repaint();
            }
        }

    }

    private JPanel build() {
        JPanel board = VisualStyle.card(new GridLayout(8, 8), BOARD_FRAME, BOARD_INSET);
        board.getAccessibleContext().setAccessibleName("Chessboard");
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                squares[row][col] = new Square(row, col);
                board.add(squares[row][col]);
            }
        }
        boardFocus = squares[6][4];
        boardFocus.setFocusable(true);
        JPanel holder = new JPanel(null) {
            @Override public void doLayout() {
                int space = Math.min(getWidth(), getHeight());
                int padding = BOARD_INSET * 2;
                int size = space < padding ? 0 : padding + (space - padding) / 8 * 8;
                board.setBounds((getWidth() - size) / 2, (getHeight() - size) / 2, size, size);
            }
        };
        holder.setOpaque(false);
        holder.add(board);
        return holder;
    }

    private Position at(int row, int col) {
        boolean reverse = flipped;
        return new Position(reverse ? 7 - row : row, reverse ? 7 - col : col);
    }

    private void focusSquare(Square square) {
        if (boardFocus != square) {
            square.setFocusable(true);
            Square previousFocus = boardFocus;
            boardFocus = square;
            square.requestFocusInWindow();
            previousFocus.setFocusable(false);
        } else {
            square.requestFocusInWindow();
        }
    }

    private static String description(Piece piece) {
        return piece == null ? "empty" : (piece.isWhite() ? "white " : "black ")
                + piece.getClass().getSimpleName().toLowerCase(Locale.ROOT);
    }

    private final class Square extends JButton {
        private static final long serialVersionUID = 1L;
        private final int row;
        private final int col;
        private transient Position pos;
        private transient Piece piece;
        private Color tile;
        private boolean legal;
        private boolean last;
        private boolean checked;
        private boolean selected;
        private boolean suggested;

        Square(int row, int col) {
            this.row = row;
            this.col = col;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusable(false);
            addActionListener(event -> {
                focusSquare(this);
                onSquare.accept(at(row, col));
            });
            bind(this, WHEN_FOCUSED, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "square-enter", this::doClick);
            int[] keys = {KeyEvent.VK_UP, KeyEvent.VK_DOWN, KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT};
            int[][] steps = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
            for (int i = 0; i < keys.length; i++) {
                int r = row + steps[i][0];
                int c = col + steps[i][1];
                if (r >= 0 && r < 8 && c >= 0 && c < 8) {
                    bind(this, WHEN_FOCUSED, KeyStroke.getKeyStroke(keys[i], 0), "square-" + keys[i],
                            () -> focusSquare(squares[r][c]));
                }
            }
        }

        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                int size = Math.min(getWidth(), getHeight());
                if (size <= 0) return;
                boolean high = VisualStyle.appearance() == VisualStyle.Appearance.HIGH_CONTRAST;
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(tile == null ? VisualStyle.palette().lightSquare() : tile);
                g.fillRect(0, 0, size, size);
                if (last) {
                    g.setColor(new Color(0x695521));
                    int corner = Math.max(6, size / 8);
                    g.fillPolygon(new int[]{size, size - corner, size}, new int[]{0, 0, corner}, 3);
                    g.fillPolygon(new int[]{0, corner, 0}, new int[]{size, size, size - corner}, 3);
                }
                if (selected || checked) {
                    g.setColor(checked ? new Color(0x8B281A) : DARK_MARK);
                    g.setStroke(new BasicStroke(2.6f));
                    g.drawRect(2, 2, size - 5, size - 5);
                    if (checked) {
                        g.setStroke(new BasicStroke(1));
                        g.drawRect(6, 6, size - 13, size - 13);
                    }
                }
                if (legal) {
                    int dot = Math.max(9, size / 5);
                    Shape marker = piece == null
                            ? new java.awt.geom.Ellipse2D.Double((size - dot) / 2.0, (size - dot) / 2.0, dot, dot)
                            : new java.awt.geom.Ellipse2D.Double(5, 5, size - 10, size - 10);
                    if (piece == null) {
                        g.setColor(DARK_MARK);
                        g.fill(marker);
                    }
                    g.setColor(high ? Color.WHITE : DARK_MARK);
                    g.setStroke(new BasicStroke(piece == null ? 1.5f : 2.5f));
                    g.draw(marker);
                    if (high && piece != null) {
                        g.setColor(DARK_MARK);
                        g.setStroke(new BasicStroke(1));
                        g.draw(marker);
                    }
                }
                if (piece != null) PieceArt.paint(g, size * .056, size * .081, size * .888,
                        piece.toString().charAt(0), piece.isWhite());
                if (pos != null) {
                    int font = Math.min(16, Math.max(11, size / 6));
                    g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, font));
                    int luminance = (tile.getRed() * 299 + tile.getGreen() * 587 + tile.getBlue() * 114) / 1000;
                    g.setColor(luminance < 135 ? Color.WHITE : DARK_MARK);
                    if (col == 0) g.drawString(Integer.toString(8 - pos.getRow()), 5, font + 4);
                    if (row == 7) g.drawString(String.valueOf((char) ('a' + pos.getCol())), size - font - 2, size - 5);
                }
                if (suggested) {
                    g.setStroke(new BasicStroke(5));
                    g.setColor(Color.WHITE);
                    g.drawRoundRect(7, 7, size - 15, size - 15, 10, 10);
                    g.setStroke(new BasicStroke(2.5f));
                    g.setColor(new Color(0x274BA5));
                    g.drawRoundRect(7, 7, size - 15, size - 15, 10, 10);
                }
                if (isFocusOwner()) {
                    g.setStroke(new BasicStroke(3));
                    g.setColor(Color.WHITE);
                    g.drawRect(4, 4, size - 9, size - 9);
                    g.setStroke(new BasicStroke(1.7f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                            10f, new float[]{3, 3}, 0));
                    g.setColor(DARK_MARK);
                    g.drawRect(4, 4, size - 9, size - 9);
                }
            } finally {
                g.dispose();
            }
        }

    }
}
