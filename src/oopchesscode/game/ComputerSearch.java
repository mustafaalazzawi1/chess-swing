package oopchesscode.game;

import oopchesscode.game.ChessMatch.Move;
import oopchesscode.game.ChessMatch.Result;
import oopchesscode.pieces.Piece;
import oopchesscode.position.Position;

import java.util.Comparator;
import java.util.List;
import java.util.function.BooleanSupplier;

final class ComputerSearch {
    enum Level {
        CASUAL("Casual", 1, 125, 1500),
        STANDARD("Standard", 2, 400, 5000),
        DEEP("Deep", 3, 850, 14000);

        final String label;
        final int depth;
        final int millis;
        final int nodes;

        Level(String label, int depth, int millis, int nodes) {
            this.label = label;
            this.depth = depth;
            this.millis = millis;
            this.nodes = nodes;
        }

        @Override public String toString() { return label; }
    }

    private static final int MATE = 100_000;
    private static final class Stopped extends RuntimeException {
        private static final long serialVersionUID = 1L;
        Stopped() { super(null, null, false, false); }
    }

    private final long deadline;
    private final int maxNodes;
    private final BooleanSupplier cancelled;
    private int nodes;

    private ComputerSearch(Level level, BooleanSupplier cancelled) {
        deadline = System.nanoTime() + level.millis * 1_000_000L;
        maxNodes = level.nodes;
        this.cancelled = cancelled;
    }

    /** Takes ownership of an already detached match; never accesses Swing or live state. */
    static Move choose(ChessMatch position, Level level, BooleanSupplier cancelled) {
        return new ComputerSearch(level, cancelled).find(position, level.depth);
    }

    private Move find(ChessMatch position, int maximumDepth) {
        if (cancelled.getAsBoolean()) return null;
        List<Move> moves = ordered(position);
        if (moves.isEmpty()) return null;
        Move best = moves.getFirst();
        try {
            for (int depth = 1; depth <= maximumDepth; depth++) {
                moves.remove(best);
                moves.addFirst(best);
                int alpha = -MATE;
                Move candidate = best;
                for (Move move : moves) {
                    checkBudget();
                    int score = -negamax(position.afterSearchMove(move), depth - 1, -MATE, -alpha, 1);
                    if (score > alpha) {
                        candidate = move;
                        alpha = score;
                    }
                }
                best = candidate;
                if (alpha >= MATE - depth) break;
            }
        } catch (Stopped ignored) {
            // Keep the last completed iteration or the first legal fallback.
        }
        return cancelled.getAsBoolean() ? null : best;
    }

    private int negamax(ChessMatch position, int depth, int alpha, int beta, int ply) {
        checkBudget();
        nodes++;
        if (position.result() == Result.CHECKMATE) return -MATE + ply;
        if (position.result() != Result.ACTIVE) return 0;
        if (depth == 0) return evaluate(position);
        int best = -MATE;
        for (Move move : ordered(position)) {
            checkBudget();
            int score = -negamax(position.afterSearchMove(move), depth - 1, -beta, -alpha, ply + 1);
            best = Math.max(best, score);
            alpha = Math.max(alpha, score);
            if (alpha >= beta) break;
        }
        return best;
    }

    private void checkBudget() {
        if (cancelled.getAsBoolean() || System.nanoTime() - deadline >= 0 || nodes >= maxNodes) throw new Stopped();
    }

    private static List<Move> ordered(ChessMatch position) {
        List<Move> moves = position.searchMoves();
        moves.sort(Comparator.comparingInt((Move move) -> {
            Piece target = position.pieceAt(move.to());
            return (target == null ? 0 : value(target)) + (move.promotion() == null ? 0 : 800);
        }).reversed());
        return moves;
    }

    private static int evaluate(ChessMatch position) {
        int whiteScore = 0;
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Piece piece = position.pieceAt(new Position(row, col));
                if (piece == null) continue;
                char type = Character.toLowerCase(piece.toString().charAt(0));
                int center = 14 - Math.abs(2 * row - 7) - Math.abs(2 * col - 7);
                int advanced = piece.isWhite() ? 6 - row : row - 1;
                int bonus = switch (type) {
                    case 'p' -> center + 3 * advanced;
                    case 'n', 'b' -> 3 * center;
                    case 'r', 'q' -> center;
                    default -> 0;
                };
                whiteScore += (value(piece) + bonus) * (piece.isWhite() ? 1 : -1);
            }
        }
        return position.whiteTurn() ? whiteScore : -whiteScore;
    }

    private static int value(Piece piece) {
        return switch (Character.toLowerCase(piece.toString().charAt(0))) {
            case 'p' -> 100;
            case 'n' -> 320;
            case 'b' -> 330;
            case 'r' -> 500;
            case 'q' -> 900;
            default -> 0;
        };
    }
}
