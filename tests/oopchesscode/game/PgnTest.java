package oopchesscode.game;

import oopchesscode.game.ChessMatch.Move;
import oopchesscode.game.ChessMatch.Result;
import oopchesscode.pieces.Knight;
import oopchesscode.pieces.Rook;
import oopchesscode.utils.Utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class PgnTest {
    public static void main(String[] args) throws Exception {
        test("SAN captures, check, mate, and both castles", PgnTest::standardSan);
        test("SAN file, rank, full-square and pinned-piece disambiguation", PgnTest::disambiguation);
        test("En passant and capture underpromotion round trips", PgnTest::specialMoves);
        test("PGN metadata, UTF-8, comments, variations and annotations", PgnTest::metadata);
        test("Result round trips for mate, stalemate, resignation, and recorded result", PgnTest::results);
        test("Invalid SAN, result, format, and FEN validation", PgnTest::invalidGames);
        test("Replay is independent of the live board and result", PgnTest::replay);
        test("Takeback restores turn, castling rights, and en passant", PgnTest::takebackSpecial);
        test("Takeback restores repetition and the 50-/75-move counters", PgnTest::takebackDraws);
        fileChecks();
        System.out.println("10 PGN, replay, takeback and file test groups passed.");
    }

    private static void standardSan() {
        Pgn.Game game = roundTrip("1.e4 e5 2.Nf3 Nc6 3.Bb5 a6 4.Bxc6 dxc6 5.O-O *");
        require(game.live().notation().equals(List.of("e4","e5","Nf3","Nc6","Bb5","a6","Bxc6","dxc6","O-O")), "SAN history");
        Pgn.Game castle = roundTrip(setup("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1", "1. O-O-O 0-0 *"));
        require(castle.live().pieceAt(Utils.ultConversion("d1")) instanceof Rook, "Queenside rook");
        require(castle.live().pieceAt(Utils.ultConversion("f8")) instanceof Rook, "Kingside rook");
        roundTrip("1. f3 e5 2. g4 Qh4# 0-1");
        roundTrip("1. e4 d5 2. Bb5+ *");
    }

    private static void disambiguation() {
        roundTrip(setup("7k/8/8/8/8/8/8/KN3N2 w - - 0 1", "1. Nbd2 *"));
        roundTrip(setup("7k/8/8/8/8/R7/8/R6K w - - 0 1", "1. R1a2 *"));
        roundTrip(setup("7k/8/8/8/8/1N6/8/KN3N2 w - - 0 1", "1. Nb1d2 *"));
        roundTrip(setup("k3r3/8/8/8/8/8/2N1N3/4K3 w - - 0 1", "1. Nd4 *"));
        reject(setup("7k/8/8/8/8/8/8/KN3N2 w - - 0 1", "1. Nd2 *"), "ambiguous");
    }

    private static void specialMoves() {
        Pgn.Game ep = roundTrip("1. e4 a6 2. e5 d5 3. exd6 *");
        require(ep.live().pieceAt(Utils.ultConversion("d5")) == null, "En passant capture removed");
        Pgn.Game promoted = roundTrip(setup("1r5k/P7/8/8/8/8/8/7K w - - 0 1", "1. axb8=N 1/2-1/2"));
        require(promoted.live().pieceAt(Utils.ultConversion("b8")) instanceof Knight, "Underpromoted knight");
        roundTrip(setup("7k/8/8/8/3pP3/8/8/7K b - e3 0 17", "17... dxe3 *"));
        roundTrip(setup("k7/6P1/8/8/8/8/8/7K w - - 0 1", "1. g8=Q+ *"));
    }

    private static void metadata() {
        String pgn = """
                %Header
                [Event "Match \\"A\\""]
                [White "José"]
                [Round "A\\\\B"]
                [Result "*"]

                1.e4! {A comment} (1. d4 (1. c4) d5) 1... e5 $1
                2. Nf3 ; move comment
                Nc6 *
                """;
        Pgn.Game game = roundTrip(String.valueOf((char) 0xFEFF) + pgn);
        require(game.tags().get("Event").equals("Match \"A\""), "Escaped quotes");
        require(game.tags().get("Round").equals("A\\B"), "Escaped backslash");
        require(game.tags().get("White").equals("José"), "UTF-8 tag");
        require(game.live().history().size() == 4, "Only the main line was loaded");
    }

    private static void results() {
        require(roundTrip("1. f3 e5 2. g4 Qh4# 0-1").live().result() == Result.CHECKMATE, "Checkmate result");
        require(roundTrip(setup("7k/5Q2/6K1/8/8/8/8/8 b - - 0 1", "1/2-1/2")).live().result()
                == Result.STALEMATE, "Stalemate result");
        Pgn.Game resigned = Pgn.parse("1. e4 *");
        resigned.live().resign();
        Pgn.Game restored = roundTrip(Pgn.write(resigned));
        require(restored.live().result() == Result.RESIGNED && restored.live().resultToken().equals("1-0"), "Resignation");
        require(roundTrip("1. e4 e5 1/2-1/2").live().result() == Result.RECORDED, "Externally recorded draw");
        require(roundTrip("1. e4 0-1").live().result() == Result.RECORDED, "Externally recorded win");
    }

    private static void invalidGames() {
        reject("1. e5 *", "illegal");
        reject("1. e2e4 *", "notation");
        reject("1. e4+ *", "should be e4");
        reject("1. f3 e5 2. g4 Qh4+ 0-1", "Qh4#");
        reject("1. f3 e5 2. g4 Qh4# 1-0", "conflicts");
        reject("1. f3 e5 2. g4 Qh4# 3. e4 0-1", "ended");
        reject("[Result \"1-0\"]\n1.e4 *", "disagrees");
        reject("[Event \"A\"]\n[Event \"B\"]\n*", "Duplicate");
        reject("[Event \"" + "a".repeat(20_000) + "\"]\n*", "overlong");
        reject("1.e4 (1.d4 *", "variation");
        reject("1.e4 {comment *", "comment");
        reject("1.e4", "Missing result");
        reject("2.e4 *", "move number");
        reject("1.e4 * 1.d4 *", "one game");
        reject("[Variant \"Chess960\"]\n*", "standard chess");
        reject("[SetUp \"1\"]\n*", "must agree");
        reject(setup("8/8/8/8/8/8/8/7K w - - 0 1", "*"), "king");
        reject(setup("7k/8/8/8/8/8/8/7K w K - 0 1", "*"), "castling");
        reject(setup("7k/8/8/8/8/8/8/7K w - d6 0 1", "*"), "pawn double step");
        reject(setup("7k/8/8/8/8/8/6k1/7K w - - 0 1", "*"), "king");
        reject(setup("7k/8/8/8/8/8/6k1/7K w - - 0 1".replace("7k", "8"), "*"), "in check");
    }

    private static void replay() {
        Pgn.Game game = Pgn.parse("1. f3 e5 2. g4 Qh4# 0-1");
        ChessMatch early = game.atPly(1);
        require(!early.whiteTurn() && early.result() == Result.ACTIVE, "Replay turn");
        require(early.play(move("d7", "d5")), "Fixtures may branch away from live play");
        require(game.live().result() == Result.CHECKMATE && game.live().history().size() == 4, "Live mate still intact");
        require(game.atPly(4).result() == Result.CHECKMATE, "Replay arrives at board-determined result");
        Pgn.Game ended = Pgn.parse("1. e4 1-0");
        require(ended.atPly(1).result() == Result.ACTIVE && ended.live().result() == Result.RECORDED, "External result remains live");
    }

    private static void takebackSpecial() {
        Pgn.Game game = Pgn.parse(setup("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1", "1. O-O *"));
        Pgn.Game restored = game.takeBack();
        require(restored.live().whiteTurn() && restored.live().play(move("e1", "c1")), "Castling right and turn restored");
        game = Pgn.parse("1. e4 a6 2. e5 d5 3. exd6 *");
        restored = game.takeBack();
        require(restored.live().pieceAt(Utils.ultConversion("d5")) != null
                && restored.live().play(move("e5", "d6")), "En passant capture restored and available");
        game = Pgn.parse("1. e4 a6 2. e5 d5 *");
        require(!game.takeBack().live().whiteTurn(), "Undo the double step returns the turn to Black");
        game = Pgn.parse("1. f3 e5 2. g4 Qh4# 0-1");
        require(game.takeBack().live().result() == Result.ACTIVE, "Takeback reopens a mated game");
    }

    private static void takebackDraws() {
        Pgn.Game game = Pgn.parse("1. Nf3 Nf6 2. Ng1 Ng8 3. Nf3 Nf6 4. Ng1 Ng8 *");
        require(game.live().claimDraw(), "Threefold reached");
        require(roundTrip(Pgn.write(game)).live().wasDrawClaimed(), "Claim survives PGN");
        Pgn.Game restored = game.takeBack();
        require(!restored.live().canClaimDraw() && restored.live().play(move("f6", "g8"))
                && restored.live().canClaimDraw(), "Repetition count rewound then rebuilt");
        game = Pgn.parse(setup("7k/8/8/8/8/8/8/R6K w - - 99 30", "30. Ra2 *"));
        require(game.live().canClaimDraw() && !game.takeBack().live().canClaimDraw(), "50-move count restored");
        game = Pgn.parse(setup("7k/8/8/8/8/8/8/R6K w - - 149 76", "76. Ra2 1/2-1/2"));
        require(game.live().result() == Result.MOVE_LIMIT && game.takeBack().live().result() == Result.ACTIVE,
                "Automatic draw rewound");
    }

    private static void fileChecks() throws IOException {
        Path folder = Files.createTempDirectory("chess-swing-test-");
        Path save = folder.resolve("game.pgn");
        Path bad = folder.resolve("invalid.pgn");
        Path large = folder.resolve("too-large.pgn");
        try {
            Pgn.Game game = Pgn.parse("1.e4 *");
            PgnFiles.write(save, game);
            require(PgnFiles.read(save).live().notation().equals(List.of("e4")), "File round trip");
            PgnFiles.write(save, Pgn.parse("1.d4 *"));
            require(PgnFiles.read(save).live().notation().equals(List.of("d4")), "Atomic replacement");
            expectIo(() -> PgnFiles.write(folder, game), "regular file");
            expectIo(() -> PgnFiles.read(folder), "regular");
            Files.write(bad, new byte[]{(byte) 0xc3, 0x28});
            expectIo(() -> PgnFiles.read(bad), "UTF-8");
            Files.write(large, new byte[PgnFiles.LIMIT + 1]);
            expectIo(() -> PgnFiles.read(large), "512 KiB");
            require(PgnFiles.read(save).live().notation().equals(List.of("d4")), "Valid save preserved");
            try (var entries = Files.list(folder)) {
                require(entries.count() == 3, "No orphaned temporary files");
            }
        } finally {
            Files.deleteIfExists(save);
            Files.deleteIfExists(bad);
            Files.deleteIfExists(large);
            Files.deleteIfExists(folder);
        }
        System.out.println("PASS PGN files, UTF-8, size limits and safe replacement");
    }

    private static Pgn.Game roundTrip(String text) {
        Pgn.Game original = Pgn.parse(text);
        Pgn.Game restored = Pgn.parse(Pgn.write(original));
        require(original.start().equals(restored.start()), "Setup round trip");
        require(original.live().history().equals(restored.live().history()), "Moves round trip");
        require(original.live().notation().equals(restored.live().notation()), "SAN round trip");
        require(original.live().result() == restored.live().result(), "Result reason round trip");
        require(original.live().resultToken().equals(restored.live().resultToken()), "Result token round trip");
        require(Pgn.write(original).equals(Pgn.write(restored)), "Deterministic export");
        return restored;
    }

    private static void reject(String text, String reason) {
        try {
            Pgn.parse(text);
            throw new AssertionError("Expected a PGN error containing " + reason);
        } catch (IllegalArgumentException error) {
            require(error.getMessage().contains(reason), error.getMessage());
        }
    }

    private static void expectIo(FileAction action, String reason) throws IOException {
        try {
            action.run();
            throw new AssertionError("Expected I/O error");
        } catch (IOException error) {
            require(error.getMessage().contains(reason), error.getMessage());
        }
    }

    @FunctionalInterface
    private interface FileAction { void run() throws IOException; }

    private static String setup(String fen, String text) {
        return "[SetUp \"1\"]\n[FEN \"" + fen + "\"]\n\n" + text;
    }

    private static Move move(String from, String to) {
        return new Move(Utils.ultConversion(from), Utils.ultConversion(to));
    }

    private static void test(String name, Runnable body) {
        body.run();
        System.out.println("PASS " + name);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
