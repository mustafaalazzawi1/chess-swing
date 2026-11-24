package oopchesscode.game;

import oopchesscode.game.ChessMatch.Move;
import oopchesscode.game.ChessMatch.Result;

/** A detached search position with a guard that can be checked on the EDT. */
record HintRequest(MatchSession owner, ChessMatch position, int ply, ChessMatch snapshot) {
    static boolean available(MatchSession session) {
        return session.live().result() == Result.ACTIVE && !session.computerTurn()
                && !session.clock.expired(session.live().whiteTurn());
    }

    static HintRequest capture(MatchSession session) {
        ChessMatch position = session.live();
        return new HintRequest(session, position, position.history().size(), position.searchCopy());
    }

    boolean accepts(MatchSession current, boolean liveView, Move move) {
        return liveView && move != null && owner == current && position == current.live()
                && ply == position.history().size() && available(current)
                && position.legalMovesFrom(move.from()).contains(move);
    }
}
