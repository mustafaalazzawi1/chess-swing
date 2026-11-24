package oopchesscode.pieces;

import oopchesscode.board.Board;
import oopchesscode.position.Position;


/**
 * Represents the Queen chess piece.
 * Combines the movement capabilities of the Rook and Bishop.
 */
public class Queen extends Piece {

    /**
     * Constructs a Queen with a given color and position.
     *
     * @param isWhite  True if the Queen is white; false if black.
     * @param position The initial position of the Queen.
     */
    public Queen(boolean isWhite, Position position) {
        super(isWhite, position);
    }

    /**
     * Determines if the Queen can move to the target position.
     * Valid moves are straight lines or diagonals without jumping over other pieces.
     *
     * @param board  The current state of the board.
     * @param target The target position to move to.
     * @return True if the move is valid; otherwise, false.
     */
    @Override
    public boolean isValidMove(Board board, Position target) {
        if (!board.isValidPosition(target)) return false;
        if (target.equals(position)) return false;

        int rowDiff = target.getRow() - position.getRow();
        int colDiff = target.getCol() - position.getCol();
        int rowStep = Integer.compare(rowDiff, 0);
        int colStep = Integer.compare(colDiff, 0);

        // Must move in a straight line or diagonal
        if (rowDiff != 0 && colDiff != 0 && Math.abs(rowDiff) != Math.abs(colDiff)) {
            return false;
        }

        int r = position.getRow() + rowStep;
        int c = position.getCol() + colStep;

        // Check if path is clear
        while (r != target.getRow() || c != target.getCol()) {
            if (!board.isEmpty(new Position(r, c))) {
                return false;
            }
            r += rowStep;
            c += colStep;
        }

        Piece dest = board.getPiece(target);
        return dest == null || dest.isWhite() != this.isWhite;
    }


    /**
     * Returns a single-character representation of the Queen.
     *
     * @return "Q" for white queen, "q" for black queen.
     */
    @Override
    public String toString() {
        return isWhite ? "Q" : "q";
    }
}
