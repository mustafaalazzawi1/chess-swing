package oopchesscode.pieces;

import oopchesscode.board.Board;
import oopchesscode.position.Position;


/**
 * Represents a King chess piece.
 * The King can move one square in any direction.
 */
public class King extends Piece {

    /**
     * Constructs a King piece.
     *
     * @param isWhite  True if the King is white, false if black.
     * @param position The initial position of the King.
     */
    public King(boolean isWhite, Position position) {
        super(isWhite, position);
    }

    /**
     * Validates whether the King can move to the target position.
     * The King can move one square in any direction unless it results in staying still,
     * and it cannot capture a piece of the same color.
     *
     * @param board  The current board state.
     * @param target The target position.
     * @return True if the move is valid.
     */
    @Override
    public boolean isValidMove(Board board, Position target) {
        int dr = Math.abs(target.getRow() - position.getRow());
        int dc = Math.abs(target.getCol() - position.getCol());

        if (dr <= 1 && dc <= 1 && !(dr == 0 && dc == 0)) {
            Piece targetPiece = board.getPiece(target);
            return targetPiece == null || targetPiece.isWhite() != this.isWhite;
        }

        return false;
    }


    /**
     * Returns a string representation of the King.
     *
     * @return "K" for white King, "k" for black King.
     */
    @Override
    public String toString() {
        return isWhite ? "K" : "k";
    }
}
