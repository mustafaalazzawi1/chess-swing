
package oopchesscode.pieces;

import oopchesscode.board.Board;
import oopchesscode.position.Position;


public class Knight extends Piece {
    public Knight(boolean isWhite, Position position) {
        super(isWhite, position);
    }

    @Override
    public boolean isValidMove(Board board, Position target) {
        int dr = Math.abs(target.getRow() - position.getRow());
        int dc = Math.abs(target.getCol() - position.getCol());
        return dr * dc == 2;
    }


    @Override
    public String toString() {
        return isWhite ? "N" : "n";
    }
}
