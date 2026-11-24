package oopchesscode.pieces;

import oopchesscode.board.Board;
import oopchesscode.position.Position;


/**
 * Represents a Pawn chess piece.
 * Handles standard pawn movement: forward 1 or 2 squares and diagonal captures.
 */
public class Pawn extends Piece {

    /**
     * Constructs a Pawn with the specified color and starting position.
     *
     * @param isWhite  True if the Pawn is white, false if black.
     * @param position The starting position of the Pawn.
     */
    public Pawn(boolean isWhite, Position position) {
        super(isWhite, position);
    }

    /**
     * Validates a move for the Pawn.
     * - 1 square forward if empty
     * - 2 squares forward from starting row if both squares are empty
     * - Diagonal capture if enemy piece is present
     *
     * @param board  The current board state.
     * @param target The target position.
     * @return True if the move is legal.
     */
    @Override
    public boolean isValidMove(Board board, Position target) {
        int dir = isWhite ? -1 : 1; // White moves up, black moves down
        int startRow = isWhite ? 6 : 1;

        int rowDiff = target.getRow() - position.getRow();
        int colDiff = target.getCol() - position.getCol();

        Piece targetPiece = board.getPiece(target);

        // Move forward 1 square
        if (colDiff == 0 && rowDiff == dir && board.isEmpty(target)) {
            return true;
        }

        // Move forward 2 squares from start position
        if (colDiff == 0 && rowDiff == 2 * dir && position.getRow() == startRow) {
            Position middle = new Position(position.getRow() + dir, position.getCol());
            return board.isEmpty(middle) && board.isEmpty(target);
        }

        // Diagonal capture
        if (Math.abs(colDiff) == 1 && rowDiff == dir && targetPiece != null && targetPiece.isWhite() != this.isWhite) {
            return true;
        }

        return false;
    }


    /**
     * Returns a string representing the Pawn.
     *
     * @return "P" for white Pawn, "p" for black Pawn.
     */
    @Override
    public String toString() {
        return isWhite ? "P" : "p";
    }
}
