package oopchesscode.game;

import oopchesscode.game.ChessMatch.Move;
import oopchesscode.game.ChessMatch.Result;

import java.util.List;
import java.util.stream.Collectors;

/** In-memory practice state. It has no connection to a live match or persistence. */
final class PuzzleSession {
    record Puzzle(String title, String fen, int mateIn, String idea) {
        ChessMatch position() { return StartPosition.parse(fen).newMatch(); }
    }

    static final List<Puzzle> PACK = List.of(
            new Puzzle("Back-rank rook", "6k1/5ppp/8/8/8/8/5PPP/4R1K1 w - - 0 1", 1,
                    "The pawns block the king's escape. Check along the back rank."),
            new Puzzle("Protected queen", "k7/8/1QK5/8/8/8/8/8 w - - 0 1", 1,
                    "Put the queen next to the cornered king with your king's protection."),
            new Puzzle("Four finishes", "7k/5Q2/6K1/8/8/8/8/8 w - - 0 1", 1,
                    "Your king covers the flight squares. Several queen checks finish the job."),
            new Puzzle("Black's back rank", "4r1k1/5ppp/8/8/8/8/5PPP/6K1 b - - 0 1", 1,
                    "Look at the escape squares of the white king, then use the open file."),
            new Puzzle("Promotion finish", "k7/2P5/1K6/8/8/8/8/8 w - - 0 1", 1,
                    "Promote to a piece that checks along the eighth rank."),
            new Puzzle("Queen checks first", "8/3k2K1/R7/8/p1Q5/8/8/8 w - - 0 1", 2,
                    "Find a queen check that funnels both king replies into a final mate."),
            new Puzzle("Quiet alternatives", "8/2p5/8/2Q3K1/8/R7/8/5k2 w - - 0 1", 2,
                    "A quiet move can fence in the king while threatening mate on your next turn."),
            new Puzzle("Bishop covers the exits", "3k2K1/3p4/4Q3/4B3/8/8/8/8 w - - 0 1", 2,
                    "Use the bishop's diagonal and move the queen so both replies can be mated."));

    private int index;
    private int remaining;
    private ChessMatch board;
    private List<Move> solutions;
    private String feedback;
    private boolean solved;

    PuzzleSession() { reset(); }

    Puzzle puzzle() { return PACK.get(index); }
    String counter() { return (index + 1) + " / " + PACK.size(); }
    ChessMatch board() { return board; }
    String feedback() { return feedback; }
    boolean solved() { return solved; }
    List<Move> solutions() { return solutions; }

    void reset() {
        board = puzzle().position();
        remaining = puzzle().mateIn();
        solutions = winningMoves(board, remaining);
        feedback = (board.whiteTurn() ? "White" : "Black") + " to move. Find the forced mate.";
        solved = false;
    }

    void next() {
        index = (index + 1) % PACK.size();
        reset();
    }

    String explanation() {
        if (solved) return puzzle().idea();
        String moves = solutions.stream().map(move -> {
            ChessMatch next = board.afterSearchMove(move);
            return board.sanStem(move) + (next.result() == Result.CHECKMATE ? "#"
                    : next.checkedKing() == null ? "" : "+");
        }).collect(Collectors.joining(", "));
        return puzzle().idea() + "\n\nWinning " + (solutions.size() == 1 ? "move: " : "moves: ") + moves;
    }

    void tryMove(Move move) {
        if (solved) return;
        if (!solutions.contains(move)) {
            feedback = "Try again. That move does not force mate.";
            return;
        }
        board.play(move);
        if (board.result() == Result.CHECKMATE) {
            solved = true;
            feedback = "Solved! Checkmate.";
            return;
        }
        // Choose the defense that leaves the fewest immediate mating replies.
        Move defense = board.searchMoves().stream().min(java.util.Comparator.comparingInt(reply ->
                winningMoves(board.afterSearchMove(reply), remaining - 1).size())).orElseThrow();
        board.play(defense);
        remaining--;
        solutions = winningMoves(board, remaining);
        feedback = "Defense: " + board.notation().getLast() + ". Find mate now.";
    }

    static List<Move> winningMoves(ChessMatch position, int movesLeft) {
        return position.searchMoves().stream().filter(move -> forcesMate(position.afterSearchMove(move), movesLeft)).toList();
    }

    private static boolean forcesMate(ChessMatch defense, int movesLeft) {
        if (defense.result() == Result.CHECKMATE) return true;
        if (movesLeft <= 1 || defense.result() != Result.ACTIVE) return false;
        List<Move> replies = defense.searchMoves();
        return !replies.isEmpty() && replies.stream().allMatch(reply -> {
            ChessMatch followUp = defense.afterSearchMove(reply);
            return followUp.searchMoves().stream().anyMatch(
                    move -> forcesMate(followUp.afterSearchMove(move), movesLeft - 1));
        });
    }
}
