
package oopchesscode.player;

import oopchesscode.board.Board;
import oopchesscode.pieces.*;
import oopchesscode.position.Position;


public class Player {
    private final boolean isWhite;

    public Player(boolean isWhite) {
        this.isWhite = isWhite;
    }

    public void setUpPawns(Board board) {
        int row = isWhite ? 6 : 1;
        for (int col = 0; col < 8; col++) {
            Position pos = new Position(row, col);
            Pawn pawn = new Pawn(isWhite, pos);
            board.setPiece(pos, pawn);
        }
    }

    public void setUpBackRow(Board board) {
        int row = isWhite ? 7 : 0;
        Piece[] pieces = {
            new Rook(isWhite, new Position(row, 0)),
            new Knight(isWhite, new Position(row, 1)),
            new Bishop(isWhite, new Position(row, 2)),
            new Queen(isWhite, new Position(row, 3)),
            new King(isWhite, new Position(row, 4)),
            new Bishop(isWhite, new Position(row, 5)),
            new Knight(isWhite, new Position(row, 6)),
            new Rook(isWhite, new Position(row, 7))
        };

        for (Piece piece : pieces) {
            board.setPiece(piece.getPosition(), piece);
        }
    }

    @Override
    public String toString() {
        return isWhite ? "White" : "Black";
    }
}
