package oopchesscode.game;

import oopchesscode.board.Board;
import oopchesscode.pieces.*;
import oopchesscode.player.Player;
import oopchesscode.position.Position;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Owns the shared rules and state for a local two-player game. */
public final class ChessMatch {
    public enum Promotion {
        QUEEN, ROOK, BISHOP, KNIGHT;

        Piece create(boolean white, Position position) {
            return switch (this) {
                case QUEEN -> new Queen(white, position);
                case ROOK -> new Rook(white, position);
                case BISHOP -> new Bishop(white, position);
                case KNIGHT -> new Knight(white, position);
            };
        }
    }

    public record Move(Position from, Position to, Promotion promotion) {
        public Move(Position from, Position to) {
            this(from, to, null);
        }
    }

    public enum Result {
        ACTIVE, CHECKMATE, STALEMATE, DEAD_MATERIAL, REPETITION, MOVE_LIMIT, RESIGNED, TIME, RECORDED
    }

    static final int ALL_CASTLING_RIGHTS = 15;
    private final Board board;
    private final Map<String, Integer> repetitions = new HashMap<>();
    private final List<Move> history = new ArrayList<>();
    private final List<String> notation = new ArrayList<>();
    private final List<Character> captures = new ArrayList<>();
    private String recordedResult;
    private boolean drawClaimed;
    private boolean timeDraw;
    private boolean whiteTurn;
    private int castlingRights;
    private Position enPassant;
    private int halfmoves;
    private int currentRepetitions;
    private Result result = Result.ACTIVE;

    public ChessMatch() {
        this(initialBoard(), true, ALL_CASTLING_RIGHTS, null, 0);
    }

    ChessMatch(Board board, boolean whiteTurn, int rights, Position enPassant, int halfmoves) {
        this.board = board;
        this.whiteTurn = whiteTurn;
        this.castlingRights = rights;
        this.enPassant = enPassant;
        this.halfmoves = halfmoves;
        recordPosition();
        updateResult();
    }

    private static Board initialBoard() {
        Board board = new Board();
        for (boolean white : new boolean[]{true, false}) {
            Player player = new Player(white);
            player.setUpBackRow(board);
            player.setUpPawns(board);
        }
        return board;
    }

    public Piece pieceAt(Position pos) { return board.getPiece(pos); }
    public boolean whiteTurn() { return whiteTurn; }
    public Result result() { return result; }
    public List<Move> history() { return List.copyOf(history); }
    public List<String> notation() { return List.copyOf(notation); }
    String capturedBy(boolean white) {
        StringBuilder pieces = new StringBuilder();
        for (char capture : captures) {
            if (Character.isLowerCase(capture) == white) pieces.append(capture);
        }
        return pieces.toString();
    }

    String notationAt(int index) { return notation.get(index); }
    boolean wasDrawClaimed() { return drawClaimed; }

    String resultToken() {
        return switch (result) {
            case ACTIVE -> "*";
            case CHECKMATE, RESIGNED -> whiteTurn ? "0-1" : "1-0";
            case TIME -> timeDraw ? "1/2-1/2" : whiteTurn ? "0-1" : "1-0";
            case RECORDED -> recordedResult;
            default -> "1/2-1/2";
        };
    }

    void recordResult(String token) {
        recordedResult = token;
        result = Result.RECORDED;
    }

    public Position checkedKing() { return inCheck(whiteTurn) ? kingSquare(whiteTurn) : null; }

    public List<Move> legalMovesFrom(Position from) {
        if (result != Result.ACTIVE) return List.of();
        Piece piece = pieceAt(from);
        if (piece == null || piece.isWhite() != whiteTurn) return List.of();
        List<Move> moves = new ArrayList<>();
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Position to = new Position(row, col);
                if (isLegal(from, to)) addMoves(moves, piece, from, to);
            }
        }
        return moves;
    }

    public boolean play(Move move) {
        if (result != Result.ACTIVE || !isLegal(move.from(), move.to())) return false;
        Piece piece = pieceAt(move.from());
        if (promotes(piece, move.to()) != (move.promotion() != null)) return false;

        advance(move, piece, true);
        return true;
    }

    private void advance(Move move, Piece piece, boolean recordMove) {
        Position capturedAt = captureSquare(piece, move.to());
        Piece captured = pieceAt(capturedAt);
        String stem = recordMove ? sanStem(move) : "";
        updateCastlingRights(piece, move.from());
        updateCastlingRights(captured, capturedAt);
        apply(piece, move.from(), move.to());
        if (move.promotion() != null) {
            board.setPiece(move.to(), move.promotion().create(whiteTurn, move.to()));
        }
        enPassant = null;
        if (piece instanceof Pawn && Math.abs(move.from().getRow() - move.to().getRow()) == 2) {
            enPassant = new Position((move.from().getRow() + move.to().getRow()) / 2, move.from().getCol());
        }
        halfmoves = piece instanceof Pawn || captured != null ? 0 : halfmoves + 1;
        if (recordMove) {
            history.add(move);
            if (captured != null) captures.add(captured.toString().charAt(0));
        }
        whiteTurn = !whiteTurn;
        recordPosition();
        updateResult();
        if (recordMove) notation.add(stem + (result == Result.CHECKMATE ? "#" : inCheck(whiteTurn) ? "+" : ""));
    }

    public boolean claimDraw() {
        if (result != Result.ACTIVE) return false;
        if (currentRepetitions >= 3) result = Result.REPETITION;
        else if (halfmoves >= 100) result = Result.MOVE_LIMIT;
        drawClaimed = result != Result.ACTIVE;
        return drawClaimed;
    }

    public boolean canClaimDraw() {
        return result == Result.ACTIVE && (halfmoves >= 100 || currentRepetitions >= 3);
    }

    public void resign() {
        if (result == Result.ACTIVE) result = Result.RESIGNED;
    }

    void flagFall() {
        if (result != Result.ACTIVE) return;
        int winningMinors = 0;
        int flaggedOtherPieces = 0;
        boolean winningMajorOrPawn = false;
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Piece piece = pieceAt(new Position(row, col));
                if (piece == null || piece instanceof King) continue;
                if (piece.isWhite() == whiteTurn) flaggedOtherPieces++;
                else if (piece instanceof Bishop || piece instanceof Knight) winningMinors++;
                else winningMajorOrPawn = true;
            }
        }
        timeDraw = !winningMajorOrPawn && (winningMinors == 0 || winningMinors == 1 && flaggedOtherPieces == 0);
        result = Result.TIME;
    }

    private ChessMatch(ChessMatch source) {
        board = new Board();
        whiteTurn = source.whiteTurn;
        castlingRights = source.castlingRights;
        enPassant = source.enPassant;
        halfmoves = source.halfmoves;
        currentRepetitions = source.currentRepetitions;
        result = source.result;
        repetitions.putAll(source.repetitions);
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Position square = new Position(row, col);
                Piece piece = source.pieceAt(square);
                if (piece == null) continue;
                boolean white = piece.isWhite();
                Piece copy = switch (Character.toLowerCase(piece.toString().charAt(0))) {
                    case 'p' -> new Pawn(white, square);
                    case 'n' -> new Knight(white, square);
                    case 'b' -> new Bishop(white, square);
                    case 'r' -> new Rook(white, square);
                    case 'q' -> new Queen(white, square);
                    case 'k' -> new King(white, square);
                    default -> throw new IllegalStateException("Unknown piece.");
                };
                board.setPiece(square, copy);
            }
        }
    }

    ChessMatch searchCopy() { return new ChessMatch(this); }

    ChessMatch afterSearchMove(Move move) {
        ChessMatch child = searchCopy();
        child.advance(move, child.pieceAt(move.from()), false);
        return child;
    }

    List<Move> searchMoves() {
        List<Move> moves = new ArrayList<>();
        if (result != Result.ACTIVE) return moves;
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) moves.addAll(legalMovesFrom(new Position(row, col)));
        }
        return moves;
    }

    public String status() {
        String turn = whiteTurn ? "White" : "Black";
        String winner = whiteTurn ? "Black" : "White";
        return switch (result) {
            case ACTIVE -> turn + " to move" + (checkedKing() == null ? "" : " · Check");
            case CHECKMATE -> winner + " wins by checkmate";
            case STALEMATE -> "Draw by stalemate";
            case DEAD_MATERIAL -> "Draw by insufficient material";
            case REPETITION -> currentRepetitions >= 5 ? "Draw by fivefold repetition" : "Draw by threefold claim";
            case MOVE_LIMIT -> halfmoves >= 150 ? "Draw by 75-move rule" : "Draw by 50-move claim";
            case RESIGNED -> winner + " wins by resignation";
            case TIME -> timeDraw ? "Draw on time · no mating material" : winner + " wins on time";
            case RECORDED -> switch (recordedResult) {
                case "1-0" -> "White wins · recorded";
                case "0-1" -> "Black wins · recorded";
                default -> "Draw · recorded";
            };
        };
    }

    String sanStem(Move move) {
        Position from = move.from();
        Position to = move.to();
        Piece piece = pieceAt(from);
        if (isCastle(piece, from, to)) return to.getCol() == 6 ? "O-O" : "O-O-O";
        boolean capture = pieceAt(captureSquare(piece, to)) != null;
        StringBuilder san = new StringBuilder();
        if (piece instanceof Pawn) {
            if (capture) san.append((char) ('a' + from.getCol()));
        } else {
            san.append(Character.toUpperCase(piece.toString().charAt(0)));
            boolean rival = false;
            boolean sameFile = false;
            boolean sameRank = false;
            for (int row = 0; row < 8; row++) {
                for (int col = 0; col < 8; col++) {
                    Position other = new Position(row, col);
                    Piece candidate = pieceAt(other);
                    if (candidate == null || candidate.getClass() != piece.getClass()
                            || candidate.isWhite() != whiteTurn || other.equals(from)
                            || !isLegal(other, to)) continue;
                    rival = true;
                    sameFile |= col == from.getCol();
                    sameRank |= row == from.getRow();
                }
            }
            if (rival && (!sameFile || sameRank)) san.append((char) ('a' + from.getCol()));
            if (rival && sameFile) san.append(8 - from.getRow());
        }
        if (capture) san.append('x');
        san.append(to);
        if (move.promotion() != null) {
            san.append('=').append(move.promotion() == Promotion.KNIGHT ? 'N' : move.promotion().name().charAt(0));
        }
        return san.toString();
    }

    List<Move> legalMovesTo(Position to) {
        List<Move> moves = new ArrayList<>();
        if (result != Result.ACTIVE) return moves;
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Position from = new Position(row, col);
                if (isLegal(from, to)) addMoves(moves, pieceAt(from), from, to);
            }
        }
        return moves;
    }

    private static void addMoves(List<Move> moves, Piece piece, Position from, Position to) {
        if (promotes(piece, to)) {
            for (Promotion promotion : Promotion.values()) moves.add(new Move(from, to, promotion));
        } else {
            moves.add(new Move(from, to));
        }
    }

    boolean isInCheck(boolean white) { return inCheck(white); }

    void display() { board.display(); }

    private boolean isLegal(Position from, Position to) {
        Piece piece = pieceAt(from);
        if (piece == null || piece.isWhite() != whiteTurn || !board.isValidPosition(to) || from.equals(to)) {
            return false;
        }
        Piece target = pieceAt(to);
        if (target instanceof King || (target != null && target.isWhite() == whiteTurn)) return false;

        if (isCastle(piece, from, to)) {
            if (!canCastle(from, to)) return false;
        } else if (!isEnPassant(piece, to) && !piece.isValidMove(board, to)) {
            return false;
        }
        return keepsKingSafe(piece, from, to);
    }

    private boolean keepsKingSafe(Piece piece, Position from, Position to) {
        Position capturedAt = captureSquare(piece, to);
        Piece captured = pieceAt(capturedAt);
        boolean castle = isCastle(piece, from, to);
        apply(piece, from, to);
        try {
            return !inCheck(piece.isWhite());
        } finally {
            board.setPiece(to, null);
            board.setPiece(from, piece);
            board.setPiece(capturedAt, captured);
            if (castle) {
                Position rookStart = rookHome(from, to);
                Position rookEnd = castleTransit(from, to);
                board.setPiece(rookStart, pieceAt(rookEnd));
                board.setPiece(rookEnd, null);
            }
        }
    }

    private void apply(Piece piece, Position from, Position to) {
        board.setPiece(captureSquare(piece, to), null);
        board.setPiece(from, null);
        board.setPiece(to, piece);
        if (isCastle(piece, from, to)) {
            Position rookStart = rookHome(from, to);
            board.setPiece(castleTransit(from, to), pieceAt(rookStart));
            board.setPiece(rookStart, null);
        }
    }

    private Position captureSquare(Piece piece, Position to) {
        return isEnPassant(piece, to) ? new Position(piece.getPosition().getRow(), to.getCol()) : to;
    }

    private boolean isEnPassant(Piece piece, Position to) {
        if (!(piece instanceof Pawn) || !to.equals(enPassant) || !board.isEmpty(to)) return false;
        Position from = piece.getPosition();
        Piece adjacent = pieceAt(new Position(from.getRow(), to.getCol()));
        return attacksWithPawn(piece, to) && adjacent instanceof Pawn && adjacent.isWhite() != piece.isWhite();
    }

    private boolean isCastle(Piece piece, Position from, Position to) {
        return piece instanceof King && from.getRow() == to.getRow()
                && Math.abs(to.getCol() - from.getCol()) == 2;
    }

    private boolean canCastle(Position from, Position to) {
        int home = whiteTurn ? 7 : 0;
        Position rookStart = rookHome(from, to);
        Piece rook = pieceAt(rookStart);
        return from.getRow() == home && from.getCol() == 4
                && (castlingRights & rookRight(whiteTurn, rookStart.getCol())) != 0
                && rook instanceof Rook && rook.isWhite() == whiteTurn
                && board.isPathClear(from, rookStart) && !inCheck(whiteTurn)
                && keepsKingSafe(pieceAt(from), from, castleTransit(from, to));
    }

    private static Position rookHome(Position from, Position to) {
        return new Position(from.getRow(), to.getCol() > from.getCol() ? 7 : 0);
    }

    private static Position castleTransit(Position from, Position to) {
        return new Position(from.getRow(), (from.getCol() + to.getCol()) / 2);
    }

    private void updateCastlingRights(Piece piece, Position from) {
        if (piece instanceof King) castlingRights &= ~(piece.isWhite() ? 3 : 12);
        if (piece instanceof Rook && from.getRow() == (piece.isWhite() ? 7 : 0)
                && (from.getCol() == 0 || from.getCol() == 7)) {
            castlingRights &= ~rookRight(piece.isWhite(), from.getCol());
        }
    }

    private static int rookRight(boolean white, int col) {
        return 1 << ((white ? 0 : 2) + (col == 7 ? 1 : 0));
    }

    private static boolean promotes(Piece piece, Position to) {
        return piece instanceof Pawn && to.getRow() == (piece.isWhite() ? 0 : 7);
    }

    private Position kingSquare(boolean white) {
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Position square = new Position(row, col);
                Piece piece = pieceAt(square);
                if (piece instanceof King && piece.isWhite() == white) return square;
            }
        }
        throw new IllegalStateException("Missing king");
    }

    private boolean inCheck(boolean white) {
        Position king = kingSquare(white);
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Piece piece = pieceAt(new Position(row, col));
                if (piece != null && piece.isWhite() != white
                        && (piece instanceof Pawn ? attacksWithPawn(piece, king) : piece.isValidMove(board, king))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean attacksWithPawn(Piece pawn, Position to) {
        Position from = pawn.getPosition();
        return to.getRow() - from.getRow() == (pawn.isWhite() ? -1 : 1)
                && Math.abs(to.getCol() - from.getCol()) == 1;
    }

    private boolean hasLegalMove() {
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                if (!legalMovesFrom(new Position(row, col)).isEmpty()) return true;
            }
        }
        return false;
    }

    private void updateResult() {
        if (!hasLegalMove()) result = inCheck(whiteTurn) ? Result.CHECKMATE : Result.STALEMATE;
        else if (deadMaterial()) result = Result.DEAD_MATERIAL;
        else if (currentRepetitions >= 5) result = Result.REPETITION;
        else if (halfmoves >= 150) result = Result.MOVE_LIMIT;
    }

    private boolean deadMaterial() {
        int knights = 0;
        int bishops = 0;
        int bishopColor = -1;
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Piece piece = pieceAt(new Position(row, col));
                if (piece == null || piece instanceof King) continue;
                if (piece instanceof Knight) {
                    knights++;
                } else if (piece instanceof Bishop) {
                    int color = (row + col) % 2;
                    if (bishopColor >= 0 && bishopColor != color) return false;
                    bishopColor = color;
                    bishops++;
                } else {
                    return false;
                }
            }
        }
        return knights + bishops <= 1 || knights == 0;
    }

    private void recordPosition() {
        currentRepetitions = repetitions.merge(positionKey(), 1, Integer::sum);
    }

    private String positionKey() {
        StringBuilder key = new StringBuilder();
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Piece piece = pieceAt(new Position(row, col));
                key.append(piece == null ? "." : piece);
            }
        }
        key.append(whiteTurn).append(':').append(castlingRights);
        if (enPassant != null) {
            int row = enPassant.getRow() + (whiteTurn ? 1 : -1);
            for (int delta : new int[]{-1, 1}) {
                Position from = new Position(row, enPassant.getCol() + delta);
                if (pieceAt(from) instanceof Pawn && isLegal(from, enPassant)) {
                    key.append(':').append(enPassant);
                    break;
                }
            }
        }
        return key.toString();
    }
}
