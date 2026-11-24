package oopchesscode.game;

import oopchesscode.game.ChessMatch.Move;
import oopchesscode.game.ChessMatch.Promotion;
import oopchesscode.game.ChessMatch.Result;
import oopchesscode.game.ComputerSearch.Level;
import oopchesscode.game.MatchClock.Control;
import oopchesscode.game.MatchClock.Time;
import oopchesscode.game.MatchSession.Opponent;
import oopchesscode.game.MatchSession.Setup;
import oopchesscode.pieces.Knight;
import oopchesscode.pieces.Rook;
import oopchesscode.utils.Utils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.LongSupplier;

public final class PortfolioTest {
    private static final long SECOND = 1_000_000_000L;

    public static void main(String[] args) {
        test("Computer search finds a legal move without mutating the live game", PortfolioTest::legalSearch);
        test("Computer search prefers checkmate and exits on cancellation", PortfolioTest::mateAndCancel);
        test("Search branches preserve castling, en passant, and promotion", PortfolioTest::specialSearch);
        test("Side selection and computer takebacks return to the human decision", PortfolioTest::computerSide);
        test("Clock charges only the moving side and pauses through replay or dialogs", PortfolioTest::pauseAndReplay);
        test("Late computer move is rejected on flag fall and outcome exports", PortfolioTest::flagFall);
        test("Captured pieces, clock checkpoints and complete takeback state", PortfolioTest::captureAndTakeback);
        test("Recovery round trip preserves settings and time without charging offline", PortfolioTest::recovery);
        test("Untrusted PGNs and malformed recovery clock histories", PortfolioTest::invalidRecovery);
        test("Checkmate beats later clock expiry; bare king cannot win on time", PortfolioTest::terminal);
        System.out.println("10 search, clocks and session test groups passed.");
    }

    private static void legalSearch() {
        Pgn.Game game = Pgn.parse("1. e4 e5 2. Nf3 Nc6 *");
        ChessMatch live = game.live();
        ChessMatch snapshot = live.searchCopy();
        for (Level level : Level.values()) {
            Move chosen = ComputerSearch.choose(snapshot, level, () -> false);
            require(chosen != null && live.legalMovesFrom(chosen.from()).contains(chosen), "Legal " + level + " move");
            require(live.notation().equals(List.of("e4", "e5", "Nf3", "Nc6")), "Original history untouched");
        }
        require(live.pieceAt(pos("a2")) != snapshot.pieceAt(pos("a2")), "No shared piece objects");
        require(ComputerSearch.choose(Pgn.parse("1. f3 e5 2. g4 Qh4# 0-1").live().searchCopy(),
                Level.CASUAL, () -> false) == null, "Terminal positions have no move");
    }

    private static void mateAndCancel() {
        ChessMatch position = StartPosition.parse("k7/8/1QK5/8/8/8/8/8 w - - 0 1").newMatch();
        Move best = ComputerSearch.choose(position.searchCopy(), Level.STANDARD, () -> false);
        require(best != null && position.afterSearchMove(best).result() == Result.CHECKMATE, "Choose mate in one");
        AtomicInteger checks = new AtomicInteger();
        Move cancelled = ComputerSearch.choose(new ChessMatch().searchCopy(), Level.DEEP,
                () -> checks.incrementAndGet() >= 8);
        require(cancelled == null && checks.get() >= 8, "Mid-search cancellation discards result");
    }

    private static void specialSearch() {
        ChessMatch position = StartPosition.parse("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1").newMatch();
        Move castle = move("e1", "g1");
        require(position.searchCopy().searchMoves().contains(castle), "Castle in search tree");
        require(position.afterSearchMove(castle).pieceAt(pos("f1")) instanceof Rook, "Rook moved on child");
        require(position.pieceAt(pos("h1")) instanceof Rook, "Root unchanged");
        ChessMatch ep = Pgn.parse("1. e4 a6 2. e5 d5 *").live().searchCopy();
        Move capture = move("e5", "d6");
        require(ep.searchMoves().contains(capture), "En passant carried into search");
        require(ep.afterSearchMove(capture).pieceAt(pos("d5")) == null, "En passant removed from branch");
        ChessMatch promotion = StartPosition.parse("7k/P7/8/8/8/8/8/7K w - - 0 1").newMatch().searchCopy();
        Move knight = new Move(pos("a7"), pos("a8"), Promotion.KNIGHT);
        require(promotion.searchMoves().contains(knight)
                && promotion.afterSearchMove(knight).pieceAt(pos("a8")) instanceof Knight, "Underpromotion in search");
    }

    private static void computerSide() {
        FakeTime now = new FakeTime();
        MatchSession game = MatchSession.create(new Setup(Opponent.COMPUTER, false, Level.STANDARD, Control.TEN), now);
        require(game.computerTurn(), "Computer opens when human is Black");
        require(game.play(move("e2", "e4")) && !game.computerTurn(), "Human now moves");
        require(game.takebackTarget() < 0, "Cannot undo only computer opener");
        now.turn(game, 4, "e7", "e5");
        require(game.computerTurn() && game.takebackTarget() == 1, "Human move can be undone during computer turn");
        now.turn(game, 1, "g1", "f3");
        require(game.takebackTarget() == 1, "Opponent reply included when taking back");
        game.takeBack();
        require(!game.computerTurn() && game.live().history().size() == 1, "Human decision restored");
        require(game.clock.reading().equals(new Time(600 * SECOND, 600 * SECOND)), "Both clocks restored");
    }

    private static void pauseAndReplay() {
        FakeTime now = new FakeTime();
        MatchSession game = local(now);
        now.turn(game, 3, "e2", "e4");
        game.clock.resume(false);
        now.add(7);
        game.clock.pause();
        now.add(120);
        require(game.clock.reading().equals(new Time(297 * SECOND, 293 * SECOND)), "Dialog time exempt");
        ChessMatch replay = game.game().atPly(0);
        require(replay.whiteTurn() && !game.live().whiteTurn(), "Replay has independent turn");
        require(game.clock.atPly(0).equals(new Time(300 * SECOND, 300 * SECOND)), "Start checkpoint");
        require(game.clock.atPly(1).equals(new Time(297 * SECOND, 300 * SECOND)), "Historical reading");
        require(!game.adjudicateTime(), "Replay wall time did not flag");
        now.turn(game, 2, "e7", "e5");
        require(game.clock.reading().black() == 291 * SECOND, "Only live time charged");
        require(!MatchSession.create(Setup.DEFAULT, now).clock.timed(), "Untimed default");
    }

    private static void flagFall() {
        FakeTime now = new FakeTime();
        MatchSession game = MatchSession.create(new Setup(Opponent.COMPUTER, true, Level.DEEP, Control.FIVE), now);
        now.turn(game, 1, "e2", "e4");
        game.clock.resume(false);
        now.add(300);
        require(!game.play(move("e7", "e5")), "Late search result rejected");
        require(game.live().history().size() == 1 && game.live().result() == Result.TIME
                && game.live().resultToken().equals("1-0"), "Computer flagged");
        Pgn.Game pgn = Pgn.parse(Pgn.write(game.exportGame()));
        require(pgn.live().result() == Result.TIME && pgn.tags().get("TimeControl").equals("300"), "PGN time result");
        require(SessionRecovery.decode(Pgn.parse(Pgn.write(SessionRecovery.encode(game))), now)
                .clock.reading().black() == 0, "Recovery preserves flag fall");
    }

    private static void captureAndTakeback() {
        FakeTime now = new FakeTime();
        MatchSession game = local(now);
        now.turn(game, 3, "e2", "e4");
        now.turn(game, 2, "a7", "a6");
        now.turn(game, 4, "e4", "e5");
        now.turn(game, 5, "d7", "d5");
        Time before = game.clock.reading();
        now.turn(game, 8, "e5", "d6");
        require(game.live().capturedBy(true).equals("p") && game.live().capturedBy(false).isEmpty(), "En passant recorded");
        require(game.game().atPly(4).capturedBy(true).isEmpty(), "Replay snapshot captures");
        game.takeBack();
        require(game.live().whiteTurn() && game.clock.reading().equals(before), "Position clocks rewound");
        require(game.live().legalMovesFrom(pos("e5")).contains(move("e5", "d6")), "Ep rights intact");
        require(game.live().capturedBy(true).isEmpty() && game.clock.checkpoints().size() == 5, "Captures and ledger truncated");
    }

    private static void recovery() {
        FakeTime now = new FakeTime();
        MatchSession game = MatchSession.create(new Setup(Opponent.COMPUTER, true, Level.DEEP, Control.TEN), now);
        now.turn(game, 14, "e2", "e4");
        game.clock.resume(false);
        now.add(3);
        String recovery = Pgn.write(SessionRecovery.encode(game));
        now.add(3600);
        MatchSession restored = SessionRecovery.decode(Pgn.parse(recovery), now);
        require(restored.setup.equals(game.setup) && restored.computerTurn(), "Recovered opponent, difficulty and turn");
        require(restored.clock.reading().equals(new Time(586 * SECOND, 597 * SECOND)), "Offline time exempt");
        require(restored.clock.atPly(1).black() == 600 * SECOND, "Historical clock still exact");
        require(!Pgn.write(restored.exportGame()).contains("ChessSwingLocal"), "Local recovery metadata stays private");
        restored.clock.resume(false);
        now.add(1);
        require(restored.clock.reading().black() == 596 * SECOND, "Resumed after startup");
    }

    private static void invalidRecovery() {
        FakeTime now = new FakeTime();
        MatchSession source = local(now);
        now.turn(source, 3, "e2", "e4");
        Pgn.Game document = SessionRecovery.encode(source);
        require(!MatchSession.imported(document, now).clock.timed(), "Ordinary PGN import starts untimed");
        require(!Pgn.write(MatchSession.imported(document, now).exportGame()).contains("ChessSwingLocal"), "Untrusted local tags removed");
        Map<String, String> broken = new java.util.LinkedHashMap<>(document.tags());
        broken.remove("ChessSwingLocalClock0");
        try {
            SessionRecovery.decode(new Pgn.Game(document.start(), broken, document.live()), now);
            throw new AssertionError("Incomplete clock ledger was accepted");
        } catch (IllegalArgumentException expected) {
            require(expected.getMessage().contains("checkpoints"), "Clear validation error");
        }
        require(!SessionRecovery.decode(Pgn.parse("1. e4 *"), now).clock.timed(), "Baseline recovery compatible");
    }

    private static void terminal() {
        FakeTime now = new FakeTime();
        MatchSession game = local(now);
        now.turn(game, 2, "f2", "f3");
        now.turn(game, 2, "e7", "e5");
        now.turn(game, 2, "g2", "g4");
        now.turn(game, 2, "d8", "h4");
        now.add(400);
        require(!game.adjudicateTime() && game.live().result() == Result.CHECKMATE, "Committed mate stands");
        ChessMatch bareKing = StartPosition.parse("7k/8/8/8/8/8/8/R6K w - - 0 1").newMatch();
        bareKing.flagFall();
        require(bareKing.resultToken().equals("1/2-1/2"), "Bare king cannot win on time");
        ChessMatch minor = StartPosition.parse("7k/8/8/8/8/4b3/8/5N1K w - - 0 1").newMatch();
        minor.flagFall();
        require(minor.resultToken().equals("0-1"), "Opponent can mate with cooperation in material considered");
    }

    private static MatchSession local(FakeTime now) {
        return MatchSession.create(new Setup(Opponent.LOCAL, true, Level.STANDARD, Control.FIVE), now);
    }

    private static final class FakeTime implements LongSupplier {
        private long time;
        @Override public long getAsLong() { return time; }
        void add(long seconds) { time += seconds * SECOND; }
        void turn(MatchSession session, long seconds, String from, String to) {
            session.clock.resume(session.live().whiteTurn());
            add(seconds);
            require(session.play(move(from, to)), "Move " + from + to);
        }
    }

    private static oopchesscode.position.Position pos(String square) { return Utils.ultConversion(square); }
    private static Move move(String from, String to) { return new Move(pos(from), pos(to)); }

    private static void test(String name, Runnable body) {
        body.run();
        System.out.println("PASS " + name);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
