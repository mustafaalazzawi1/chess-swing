package oopchesscode.pieces;

import oopchesscode.board.Board;
import oopchesscode.position.Position;


/**
 * Represents a Rook chess piece.
 * Rooks can move horizontally and vertically across the board.
 */
public class Rook extends Piece {

    /**
     * Constructs a Rook piece with its color and starting position.
     *
     * @param isWhite  True if the piece is white, false if black.
     * @param position The initial position of the Rook.
     */
    public Rook(boolean isWhite, Position position) {
        super(isWhite, position);
    }

    /**
     * Checks if a move to the target position is valid for the Rook.
     * Rooks move in straight lines horizontally or vertically with no obstructions.
     *
     * @param board  The current game board.
     * @param target The position to move to.
     * @return True if the move is valid, false otherwise.
     */
    @Override
    public boolean isValidMove(Board board, Position target) {
        if (!board.isValidPosition(target)) return false;

        // Cannot move to the same square
        if (target.equals(position)) return false;

        int fromRow = position.getRow();
        int fromCol = position.getCol();
        int toRow = target.getRow();
        int toCol = target.getCol();

        // Rook must move in a straight line (either row or column must remain constant)
        if (fromRow != toRow && fromCol != toCol) return false;

        // Determine step direction for movement
        int rowStep = Integer.compare(toRow, fromRow);
        int colStep = Integer.compare(toCol, fromCol);

        int currentRow = fromRow + rowStep;
        int currentCol = fromCol + colStep;

        // Check for obstructions along the path
        while (currentRow != toRow || currentCol != toCol) {
            if (!board.isEmpty(new Position(currentRow, currentCol))) {
                return false;
            }
            currentRow += rowStep;
            currentCol += colStep;
        }

        // Target position must be empty or contain opponent’s piece
        Piece destPiece = board.getPiece(target);
        return destPiece == null || destPiece.isWhite() != this.isWhite;
    }


    /**
     * Returns a string representation of the Rook.
     *
     * @return "R" for white Rook, "r" for black Rook.
     */
    @Override
    public String toString() {
        return isWhite ? "R" : "r";
    }
}
