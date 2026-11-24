package oopchesscode.board;

import oopchesscode.pieces.Piece;
import oopchesscode.position.Position;

public class Board {
    private final Piece[][] board = new Piece[8][8];

    public void setPiece(Position pos, Piece piece) {
        board[pos.getRow()][pos.getCol()] = piece;
        if (piece != null) piece.setPosition(pos);
    }

    public Piece getPiece(Position pos) {
        return isValidPosition(pos) ? board[pos.getRow()][pos.getCol()] : null;
    }

    public boolean isEmpty(Position pos) {
        return getPiece(pos) == null;
    }

    public boolean isValidPosition(Position pos) {
        return pos != null && pos.getRow() >= 0 && pos.getRow() < 8
                && pos.getCol() >= 0 && pos.getCol() < 8;
    }

    /** Checks an already validated straight or diagonal path, excluding its endpoints. */
    public boolean isPathClear(Position from, Position to) {
        int rowStep = Integer.compare(to.getRow(), from.getRow());
        int colStep = Integer.compare(to.getCol(), from.getCol());
        int row = from.getRow() + rowStep;
        int col = from.getCol() + colStep;
        while (row != to.getRow() || col != to.getCol()) {
            if (board[row][col] != null) return false;
            row += rowStep;
            col += colStep;
        }
        return true;
    }

    public void display() {
        for (int row = 0; row < 8; row++) {
            System.out.print((8 - row) + " ");
            for (Piece piece : board[row]) System.out.print((piece == null ? "." : piece) + " ");
            System.out.println();
        }
        System.out.println("  a b c d e f g h");
    }
}
