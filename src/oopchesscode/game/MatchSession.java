package oopchesscode.game;

import oopchesscode.game.ChessMatch.Move;
import oopchesscode.game.ChessMatch.Result;
import oopchesscode.game.ComputerSearch.Level;
import oopchesscode.game.MatchClock.Control;

import java.util.Map;
import java.util.function.LongSupplier;

final class MatchSession {
    enum Opponent {
        LOCAL("Local two-player"), COMPUTER("Computer opponent");

        final String label;
        Opponent(String label) { this.label = label; }
        @Override public String toString() { return label; }
    }

    record Setup(Opponent opponent, boolean humanWhite, Level level, Control control) {
        static final Setup DEFAULT = new Setup(Opponent.LOCAL, true, Level.STANDARD, Control.UNTIMED);

        Setup {
            if (opponent == Opponent.LOCAL) {
                humanWhite = true;
                level = Level.STANDARD;
            }
        }
    }

    private Pgn.Game game;
    final Setup setup;
    final MatchClock clock;

    MatchSession(Pgn.Game game, Setup setup, MatchClock clock) {
        this.game = game;
        this.setup = setup;
        this.clock = clock;
    }

    static MatchSession create(Setup setup, LongSupplier now) {
        Pgn.Game fresh = Pgn.Game.fresh();
        return new MatchSession(fresh, setup, new MatchClock(setup.control(), now));
    }

    static MatchSession imported(Pgn.Game game, LongSupplier now) {
        return new MatchSession(SessionRecovery.clean(game), Setup.DEFAULT, new MatchClock(Control.UNTIMED, now));
    }

    Pgn.Game game() { return game; }
    ChessMatch live() { return game.live(); }
    boolean versusComputer() { return setup.opponent() == Opponent.COMPUTER; }
    boolean computerTurn() { return versusComputer() && live().whiteTurn() != setup.humanWhite(); }

    boolean adjudicateTime() {
        if (live().result() != Result.ACTIVE || !clock.expired(live().whiteTurn())) return false;
        clock.pause();
        live().flagFall();
        return true;
    }

    boolean play(Move move) {
        clock.pause();
        if (adjudicateTime() || !live().play(move)) return false;
        clock.commitMove();
        return true;
    }

    int takebackTarget() {
        int plies = live().history().size();
        if (!versusComputer()) return plies - 1;
        return plies - (computerTurn() ? 1 : 2);
    }

    void takeBack() {
        int target = takebackTarget();
        if (target < 0) throw new IllegalStateException("There is no playable position to take back to.");
        game = game.takeBackTo(target);
        clock.rewind(target);
    }

    Pgn.Game exportGame() {
        if (!clock.timed()) return game;
        Map<String, String> tags = new java.util.LinkedHashMap<>(game.tags());
        tags.put("TimeControl", Long.toString(setup.control().nanos / 1_000_000_000L));
        return new Pgn.Game(game.start(), tags, game.live());
    }
}
