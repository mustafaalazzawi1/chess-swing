package oopchesscode.pieces;

import oopchesscode.board.Board;
import oopchesscode.position.Position;


/**
 * Abstract base class representing a chess piece.
 * Provides common properties and methods shared by all piece types.
 */
public abstract class Piece {
    /** Indicates whether the piece is white (true) or black (false) */
    protected boolean isWhite;

    /** The current position of the piece on the board */
    protected Position position;

    /**
     * Constructs a Piece with a color and starting position.
     *
     * @param isWhite  True if the piece is white; false if black.
     * @param position The starting position of the piece.
     */
    public Piece(boolean isWhite, Position position) {
        this.isWhite = isWhite;
        this.position = position;
    }

    /**
     * Gets the color of the piece.
     *
     * @return True if the piece is white, false if black.
     */
    public boolean isWhite() {
        return isWhite;
    }

    /**
     * Gets the current position of the piece.
     *
     * @return The current position on the board.
     */
    public Position getPosition() {
        return position;
    }

    /**
     * Updates the position of the piece.
     *
     * @param position The new position to set.
     */
    public void setPosition(Position position) {
        this.position = position;
    }

    /**
     * Determines whether the move from current position to target is valid for this piece.
     *
     * @param board  The current board state.
     * @param target The target position to move to.
     * @return True if the move is valid; otherwise, false.
     */
    public abstract boolean isValidMove(Board board, Position target);

}
