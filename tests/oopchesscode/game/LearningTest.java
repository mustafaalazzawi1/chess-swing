package oopchesscode.game;

import oopchesscode.game.ChessMatch.Move;
import oopchesscode.game.ChessMatch.Result;
import oopchesscode.game.MatchClock.Control;
import oopchesscode.game.MatchSession.Opponent;
import oopchesscode.game.MatchSession.Setup;
import oopchesscode.utils.Utils;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public final class LearningTest {
    private static final long SECOND = 1_000_000_000L;

    public static void main(String[] args) {
        check("Hint is legal and leaves live move and clock state alone", LearningTest::hint);
        check("Hint rejects stale positions, non-live views, and flag fall", LearningTest::staleHints);
        check("Hint policy allows human turns only", LearningTest::hintPolicy);
        check("Eight valid original positions with exact mate objectives", LearningTest::objectives);
        check("Every defense to every accepted mate-in-two move has a mate", LearningTest::defenses);
        check("Practice accepts alternatives and can complete every exercise", LearningTest::practice);
        check("Practice is isolated from live board, replay, clock and recovery payload", LearningTest::isolation);
        System.out.println("7 learning test groups passed.");
    }

    private static void hint() {
        AtomicLong now = new AtomicLong();
        MatchSession session = timed(now);
        session.clock.resume(true);
        String before = Pgn.write(SessionRecovery.encode(session));
        HintRequest request = HintRequest.capture(session);
        Move move = ComputerSearch.choose(request.snapshot(), ComputerSearch.Level.STANDARD, () -> false);
        require(request.accepts(session, true, move), "Legal suggestion");
        require(Pgn.write(SessionRecovery.encode(session)).equals(before), "No board or recovery payload change");
        now.addAndGet(3 * SECOND);
        require(session.clock.running() && session.clock.reading().white() == 297 * SECOND
                && session.clock.reading().black() == 300 * SECOND, "Hint did not pause or reset live clock");
        require(session.clock.checkpoints().size() == 1 && session.live().history().isEmpty(), "No move committed");
    }

    private static void staleHints() {
        AtomicLong now = new AtomicLong();
        MatchSession session = timed(now);
        HintRequest request = HintRequest.capture(session);
        Move e4 = move("e2", "e4");
        require(request.accepts(session, true, e4), "Current request");
        require(!request.accepts(session, false, e4), "Replay, home, practice, import and close suppress results");
        require(!request.accepts(timed(now), true, e4), "Same board in a new session is stale");
        require(!request.accepts(session, true, move("e2", "e5")), "Illegal result rejected");
        require(session.play(e4), "Play a live move");
        require(!request.accepts(session, true, e4), "Ply changed");
        session.takeBack();
        require(!request.accepts(session, true, e4), "Identical board after takeback still stale");
        HintRequest nearFlag = HintRequest.capture(session);
        session.clock.resume(true);
        now.set(300 * SECOND);
        require(!nearFlag.accepts(session, true, e4), "Pending flag invalidates immediately");
        require(session.live().result() == Result.ACTIVE, "Guard does not adjudicate time itself");
        require(session.adjudicateTime() && !nearFlag.accepts(session, true, e4), "Flagged game rejects hint");
    }

    private static void hintPolicy() {
        AtomicLong now = new AtomicLong();
        MatchSession session = MatchSession.create(
                new Setup(Opponent.COMPUTER, false, ComputerSearch.Level.STANDARD, Control.UNTIMED), now::get);
        require(!HintRequest.available(session), "Computer opens");
        require(session.play(move("e2", "e4")) && HintRequest.available(session), "Human Black can ask");
        session.live().resign();
        require(!HintRequest.available(session), "Finished position");
        MatchSession local = timed(now);
        require(local.play(move("e2", "e4")) && HintRequest.available(local), "Local Black can ask");
    }

    private static void objectives() {
        int one = 0;
        int two = 0;
        for (PuzzleSession.Puzzle puzzle : PuzzleSession.PACK) {
            ChessMatch position = puzzle.position();
            require(position.result() == Result.ACTIVE, puzzle.title());
            List<Move> immediate = immediateMates(position);
            if (puzzle.mateIn() == 1) {
                one++;
                require(!immediate.isEmpty(), puzzle.title());
                require(immediate.equals(PuzzleSession.winningMoves(position, 1)), puzzle.title());
            } else {
                two++;
                require(immediate.isEmpty(), "No shorter mate in " + puzzle.title());
                require(!PuzzleSession.winningMoves(position, 2).isEmpty(), "Forced mate in " + puzzle.title());
            }
        }
        require(one == 5 && two == 3, "Expected five mate-in-one and three mate-in-two puzzles");
    }

    private static void defenses() {
        for (PuzzleSession.Puzzle puzzle : PuzzleSession.PACK) {
            if (puzzle.mateIn() != 2) continue;
            ChessMatch root = puzzle.position();
            List<Move> actual = PuzzleSession.winningMoves(root, 2);
            boolean branches = false;
            for (Move attack : root.searchMoves()) {
                ChessMatch defense = root.afterSearchMove(attack);
                List<Move> replies = defense.searchMoves();
                boolean forced = !replies.isEmpty();
                for (Move reply : replies) {
                    if (immediateMates(defense.afterSearchMove(reply)).isEmpty()) forced = false;
                }
                if (forced && replies.size() > 1) branches = true;
                require(actual.contains(attack) == forced, "Forced attack classification in " + puzzle.title());
            }
            require(branches, "Multiple defender replies verified for " + puzzle.title());
        }
    }

    private static void practice() {
        PuzzleSession exercise = new PuzzleSession();
        exercise.tryMove(move("e1", "e5"));
        require(exercise.board().history().isEmpty() && exercise.feedback().contains("Try again"), "Wrong move stays put");
        int alternatives = 0;
        for (int i = 0; i < PuzzleSession.PACK.size(); i++) {
            List<Move> answers = exercise.solutions();
            if (answers.size() > 1) alternatives++;
            for (Move answer : answers) {
                exercise.tryMove(answer);
                if (exercise.puzzle().mateIn() == 2) {
                    require(exercise.board().history().size() == 2 && !exercise.solved(), "Defense is played");
                    exercise.tryMove(exercise.solutions().getLast());
                }
                require(exercise.solved() && exercise.board().result() == Result.CHECKMATE, exercise.puzzle().title());
                exercise.reset();
            }
            exercise.next();
        }
        require(alternatives >= 2 && exercise.counter().equals("1 / 8"), "Alternative wins and wraparound");
    }

    private static void isolation() {
        AtomicLong now = new AtomicLong();
        MatchSession live = timed(now);
        live.clock.resume(true);
        now.addAndGet(4 * SECOND);
        require(live.play(move("e2", "e4")), "Live move");
        live.clock.resume(false);
        now.addAndGet(3 * SECOND);
        live.clock.pause();
        String before = Pgn.write(SessionRecovery.encode(live));
        PuzzleSession practice = new PuzzleSession();
        practice.tryMove(practice.solutions().getFirst());
        now.addAndGet(30 * SECOND);
        require(Pgn.write(SessionRecovery.encode(live)).equals(before), "Practice and elapsed time did not change recovery payload");
        require(live.game().atPly(1).notation().equals(List.of("e4")), "Replay unchanged");
        live.clock.resume(false);
        now.addAndGet(2 * SECOND);
        require(live.clock.reading().black() == 295 * SECOND, "Resume deducts only live time");
    }

    private static List<Move> immediateMates(ChessMatch position) {
        return position.searchMoves().stream().filter(move -> position.afterSearchMove(move).result() == Result.CHECKMATE).toList();
    }

    private static MatchSession timed(AtomicLong now) {
        return MatchSession.create(new Setup(Opponent.LOCAL, true, ComputerSearch.Level.STANDARD, Control.FIVE), now::get);
    }

    private static Move move(String from, String to) {
        return new Move(Utils.ultConversion(from), Utils.ultConversion(to));
    }

    private static void check(String name, Runnable body) {
        body.run();
        System.out.println("PASS " + name);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
