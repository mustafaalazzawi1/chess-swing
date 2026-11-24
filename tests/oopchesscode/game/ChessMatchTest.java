package oopchesscode.game;

import oopchesscode.board.Board;
import oopchesscode.game.ChessMatch.Move;
import oopchesscode.game.ChessMatch.Promotion;
import oopchesscode.game.ChessMatch.Result;
import oopchesscode.pieces.*;
import oopchesscode.position.Position;
import oopchesscode.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class ChessMatchTest {
    public static void main(String[] args) {
        test("Reference perft (starting position and Kiwipete)", ChessMatchTest::perft);
        test("Turn ownership, friendly pieces, blocked sliders", ChessMatchTest::ordinaryMoves);
        test("Check, pins, blocks, and double-check", ChessMatchTest::check);
        test("Checkmate and terminal position lock", ChessMatchTest::checkmate);
        test("Stalemate", ChessMatchTest::stalemate);
        test("Castling on both sides and across either color", ChessMatchTest::castling);
        test("Castling restrictions and rights loss", ChessMatchTest::castlingRestrictions);
        test("En passant capture, expiration and discovered check", ChessMatchTest::enPassant);
        test("Promotion, underpromotion, and capture promotion", ChessMatchTest::promotion);
        test("Insufficient material", ChessMatchTest::deadMaterial);
        test("Threefold claim and automatic fivefold repetition", ChessMatchTest::repetition);
        test("50-move claim, automatic 75-move draw, mate precedence", ChessMatchTest::moveLimit);
        test("Resignation", ChessMatchTest::resignation);
        System.out.println("13 test groups passed.");
    }

    private static void perft() {
        checkPerft(ChessMatch::new, 20, 400, 8902);
        checkPerft(() -> position("r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0"),
                48, 2039);
    }

    private static void checkPerft(Supplier<ChessMatch> root, int... expected) {
        for (int depth = 1; depth <= expected.length; depth++) {
            int actual = nodes(root, root.get(), new ArrayList<>(), depth);
            require(actual == expected[depth - 1], "Depth " + depth + " counted " + actual);
        }
    }

    private static int nodes(Supplier<ChessMatch> root, ChessMatch match, List<Move> path, int depth) {
        List<Move> options = new ArrayList<>();
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) options.addAll(match.legalMovesFrom(new Position(row, col)));
        }
        if (depth == 1) return options.size();
        int total = 0;
        for (Move option : options) {
            path.add(option);
            ChessMatch child = root.get();
            for (Move move : path) require(child.play(move), "Perft replay");
            total += nodes(root, child, path, depth - 1);
            path.removeLast();
        }
        return total;
    }

    private static void ordinaryMoves() {
        ChessMatch match = new ChessMatch();
        illegal(match, "e7e5", "c1h6", "a1a4", "d1d3", "b1d2", "e2e5", "e2d3", "e2e2");
        moves(match, "e2e4", "d7d5", "e4d5", "g8f6", "f1b5");
        require(match.checkedKing().equals(pos("e8")), "Bishop must give check");
        require(match.pieceAt(pos("d5")) instanceof Pawn, "Capture must replace the target");
    }

    private static void check() {
        ChessMatch pin = position("k3r3/8/8/8/8/8/4R3/4K3 w - - 0");
        illegal(pin, "e2d2");
        moves(pin, "e2e8");
        ChessMatch block = position("k3r3/8/8/8/8/8/8/2B1K3 w - - 0");
        require(block.checkedKing() != null, "White starts in check");
        moves(block, "c1e3");
        ChessMatch doubled = position("k3r3/8/8/8/1b6/8/8/2B1K3 w - - 0");
        illegal(doubled, "c1e3");
        moves(doubled, "e1f1");
        ChessMatch pawn = position("7k/8/8/8/8/3p4/8/4K3 w - - 0");
        illegal(pawn, "e1e2");
        moves(pawn, "e1d2");
        ChessMatch kings = position("8/8/8/8/8/4k3/8/R3K3 w - - 0");
        illegal(kings, "e1e2");
    }

    private static void checkmate() {
        ChessMatch match = new ChessMatch();
        moves(match, "f2f3", "e7e5", "g2g4", "d8h4");
        require(match.result() == Result.CHECKMATE, "Fool's mate");
        require(match.status().equals("Black wins by checkmate"), "Winner should be Black");
        illegal(match, "a2a3");
        ChessMatch kingCapture = position("4k3/8/8/8/8/8/4R3/4K3 w - - 0");
        illegal(kingCapture, "e2e8");
    }

    private static void stalemate() {
        ChessMatch match = position("7k/5Q2/6K1/8/8/8/8/8 b - - 0");
        require(match.result() == Result.STALEMATE && match.checkedKing() == null, "Stalemate without check");
    }

    private static void castling() {
        ChessMatch match = position("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0");
        moves(match, "e1g1", "e8c8");
        require(match.pieceAt(pos("f1")) instanceof Rook && match.pieceAt(pos("d8")) instanceof Rook,
                "Both rooks must move");
        match = position("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0");
        moves(match, "e1c1", "e8g8");
    }

    private static void castlingRestrictions() {
        illegal(position("4k3/8/8/8/8/8/8/RN2K2R w KQ - 0"), "e1c1");
        illegal(position("k3r3/8/8/8/8/8/8/4K2R w K - 0"), "e1g1");
        illegal(position("k4r2/8/8/8/8/8/8/4K2R w K - 0"), "e1g1");
        illegal(position("k5r1/8/8/8/8/8/8/4K2R w K - 0"), "e1g1");
        ChessMatch match = position("4k3/8/8/8/8/8/8/R3K2R w KQ - 0");
        moves(match, "h1h2", "e8e7", "h2h1", "e7e8");
        illegal(match, "e1g1");
        moves(match, "e1e2", "e8e7", "e2e1", "e7e8");
        illegal(match, "e1c1");
    }

    private static void enPassant() {
        ChessMatch match = new ChessMatch();
        moves(match, "e2e4", "a7a6", "e4e5", "d7d5", "e5d6");
        require(match.pieceAt(pos("d5")) == null, "Remove bypassed pawn");
        match = new ChessMatch();
        moves(match, "e2e4", "a7a6", "e4e5", "d7d5", "a2a3", "a6a5");
        illegal(match, "e5d6");
        illegal(position("7k/8/8/r4pPK/8/8/8/8 w - f6 0"), "g5f6");
        match = position("4k3/8/8/8/3pP3/8/8/4K3 b - e3 0");
        moves(match, "d4e3");
        require(match.pieceAt(pos("e4")) == null, "Black en passant");
    }

    private static void promotion() {
        for (Promotion choice : Promotion.values()) {
            ChessMatch match = position("1r5k/P7/8/8/8/8/8/7K w - - 0");
            illegal(match, "a7a8");
            require(match.legalMovesFrom(pos("a7")).size() == 8, "Four choices for each destination");
            require(match.play(new Move(pos("a7"), pos("b8"), choice)), "Capture promotion to " + choice);
            require(match.pieceAt(pos("b8")).getClass().getSimpleName().equalsIgnoreCase(choice.name()),
                    "Use chosen piece");
        }
        ChessMatch black = position("7k/8/8/8/8/8/p7/7K b - - 0");
        require(black.play(new Move(pos("a2"), pos("a1"), Promotion.QUEEN)), "Black promotion");
    }

    private static void deadMaterial() {
        require(position("7k/8/8/8/8/8/8/7K w - - 0").result() == Result.DEAD_MATERIAL, "Bare kings");
        require(position("7k/8/8/8/8/8/8/6NK w - - 0").result() == Result.DEAD_MATERIAL, "Single knight");
        require(position("7k/8/8/8/8/4b3/8/2B4K w - - 0").result() == Result.DEAD_MATERIAL, "Same-color bishops");
        require(position("7k/8/8/8/8/3b4/8/2B4K w - - 0").result() == Result.ACTIVE, "Opposite-color bishops");
    }

    private static void repetition() {
        ChessMatch match = new ChessMatch();
        String[] cycle = {"g1f3", "g8f6", "f3g1", "f6g8"};
        require(!match.claimDraw(), "No claim initially");
        moves(match, cycle);
        moves(match, cycle);
        require(match.canClaimDraw() && match.claimDraw() && match.result() == Result.REPETITION, "Threefold claim");
        match = new ChessMatch();
        for (int i = 0; i < 4; i++) moves(match, cycle);
        require(match.result() == Result.REPETITION, "Fivefold automatic");
    }

    private static void moveLimit() {
        ChessMatch match = position("7k/8/8/8/8/8/8/R6K w - - 99");
        moves(match, "a1a2");
        require(match.claimDraw() && match.result() == Result.MOVE_LIMIT, "50-move claim");
        match = position("7k/8/8/8/8/8/8/R6K w - - 149");
        moves(match, "a1a2");
        require(match.result() == Result.MOVE_LIMIT, "75-move automatic");
        match = position("7k/5Q2/6K1/8/8/8/8/8 w - - 149");
        moves(match, "f7g7");
        require(match.result() == Result.CHECKMATE, "Mate takes precedence");
        match = position("7k/8/8/8/8/8/P7/7K w - - 149");
        moves(match, "a2a4");
        require(!match.canClaimDraw() && match.result() == Result.ACTIVE, "Pawn resets counter");
    }

    private static void resignation() {
        ChessMatch match = new ChessMatch();
        match.resign();
        require(match.status().equals("Black wins by resignation"), "Resigning side forfeits");
        illegal(match, "e2e4");
    }

    /** Builds focused rule positions from the board, turn, castling, EP, and halfmove FEN fields. */
    private static ChessMatch position(String fen) {
        String[] fields = fen.split(" ");
        String[] rows = fields[0].split("/");
        Board board = new Board();
        for (int row = 0; row < 8; row++) {
            int col = 0;
            for (char symbol : rows[row].toCharArray()) {
                if (Character.isDigit(symbol)) {
                    col += symbol - '0';
                    continue;
                }
                boolean white = Character.isUpperCase(symbol);
                Position pos = new Position(row, col++);
                Piece piece = switch (Character.toLowerCase(symbol)) {
                    case 'k' -> new King(white, pos);
                    case 'q' -> new Queen(white, pos);
                    case 'r' -> new Rook(white, pos);
                    case 'b' -> new Bishop(white, pos);
                    case 'n' -> new Knight(white, pos);
                    case 'p' -> new Pawn(white, pos);
                    default -> throw new IllegalArgumentException(fen);
                };
                board.setPiece(pos, piece);
            }
        }
        int rights = 0;
        for (int i = 0; i < 4; i++) if (fields[2].indexOf("QKqk".charAt(i)) >= 0) rights |= 1 << i;
        Position ep = fields[3].equals("-") ? null : pos(fields[3]);
        return new ChessMatch(board, fields[1].equals("w"), rights, ep, Integer.parseInt(fields[4]));
    }

    private static Position pos(String square) { return Utils.ultConversion(square); }
    private static Move move(String text) { return new Move(pos(text.substring(0, 2)), pos(text.substring(2))); }

    private static void moves(ChessMatch match, String... moves) {
        for (String text : moves) require(match.play(move(text)), "Expected legal move " + text);
    }

    private static void illegal(ChessMatch match, String... moves) {
        for (String text : moves) require(!match.play(move(text)), "Expected illegal move " + text);
    }

    private static void test(String name, Runnable body) {
        body.run();
        System.out.println("PASS " + name);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
