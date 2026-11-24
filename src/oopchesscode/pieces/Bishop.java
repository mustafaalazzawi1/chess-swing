package oopchesscode.pieces;

import oopchesscode.board.Board;
import oopchesscode.position.Position;


/**
 * Represents a Bishop chess piece.
 * Bishops can move diagonally across the board any number of squares.
 */
public class Bishop extends Piece {

    /**
     * Constructs a new Bishop.
     *
     * @param isWhite  True if the Bishop is white, false if black.
     * @param position The starting position of the Bishop.
     */
    public Bishop(boolean isWhite, Position position) {
        super(isWhite, position);
    }

    /**
     * Checks whether the Bishop can move to the target position.
     * A Bishop moves diagonally, so the absolute difference between rows
     * and columns must be equal.
     *
     * @param board  The current board state.
     * @param target The target position to move to.
     * @return True if the move is valid based on diagonal movement.
     */
    @Override
    public boolean isValidMove(Board board, Position target) {
        int rows = target.getRow() - position.getRow();
        int cols = target.getCol() - position.getCol();
        return rows != 0 && Math.abs(rows) == Math.abs(cols)
                && board.isPathClear(position, target);
    }


    /**
     * Returns a string representation of the Bishop.
     *
     * @return "B" for white Bishop, "b" for black Bishop.
     */
    @Override
    public String toString() {
        return isWhite ? "B" : "b";
    }
}
