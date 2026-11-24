package oopchesscode.game;

import oopchesscode.board.Board;
import oopchesscode.pieces.*;
import oopchesscode.position.Position;
import oopchesscode.utils.Utils;

/** Standard setup or a validated orthodox FEN setup. */
record StartPosition(String fen, int moveNumber, boolean white) {
    static final StartPosition STANDARD = new StartPosition(null, 1, true);

    static StartPosition parse(String fen) {
        if (fen == null) return STANDARD;
        String[] fields = fen.strip().split("\\s+");
        if (fields.length != 6 || !fields[1].matches("[wb]")) throw invalid("expected six fields and a side to move");
        int full = number(fields[5], 1, 1_000_000);
        StartPosition start = new StartPosition(String.join(" ", fields), full, fields[1].equals("w"));
        start.newMatch();
        return start;
    }

    int numberAt(int ply) { return moveNumber + (ply + (white ? 0 : 1)) / 2; }
    boolean whiteAt(int ply) { return white == (ply % 2 == 0); }

    ChessMatch newMatch() {
        if (fen == null) return new ChessMatch();
        String[] fields = fen.split(" ");
        Board board = parseBoard(fields[0]);
        int rights = castlingRights(board, fields[2]);
        int halfmoves = number(fields[4], 0, 150);
        Position ep = parseEnPassant(board, fields[3], halfmoves);
        ChessMatch match = new ChessMatch(board, white, rights, ep, halfmoves);
        if (match.isInCheck(!white)) throw invalid("the side that just moved is in check");
        return match;
    }

    private static Board parseBoard(String placement) {
        String[] rows = placement.split("/", -1);
        if (rows.length != 8) throw invalid("expected eight ranks");
        Board board = new Board();
        int[] kings = new int[2];
        int[] pawns = new int[2];
        int[] pieces = new int[2];
        for (int row = 0; row < 8; row++) {
            int col = 0;
            boolean previousDigit = false;
            for (char symbol : rows[row].toCharArray()) {
                if (symbol >= '1' && symbol <= '8' && !previousDigit) {
                    col += symbol - '0';
                    previousDigit = true;
                    continue;
                }
                previousDigit = false;
                if (col >= 8) throw invalid("rank has too many squares");
                if ("KQRBNPkqrbnp".indexOf(symbol) < 0) throw invalid("unknown piece or malformed empty squares");
                boolean white = Character.isUpperCase(symbol);
                int side = white ? 0 : 1;
                Position pos = new Position(row, col++);
                Piece piece = switch (Character.toLowerCase(symbol)) {
                    case 'k' -> new King(white, pos);
                    case 'q' -> new Queen(white, pos);
                    case 'r' -> new Rook(white, pos);
                    case 'b' -> new Bishop(white, pos);
                    case 'n' -> new Knight(white, pos);
                    case 'p' -> new Pawn(white, pos);
                    default -> throw invalid("unknown piece or malformed empty squares");
                };
                pieces[side]++;
                if (piece instanceof King) kings[side]++;
                if (piece instanceof Pawn) {
                    pawns[side]++;
                    if (row == 0 || row == 7) throw invalid("pawn on the first or eighth rank");
                }
                board.setPiece(pos, piece);
            }
            if (col != 8) throw invalid("each rank must contain eight squares");
        }
        for (int side = 0; side < 2; side++) {
            if (kings[side] != 1 || pawns[side] > 8 || pieces[side] > 16) {
                throw invalid("invalid king, pawn, or piece count");
            }
        }
        return board;
    }

    private static int castlingRights(Board board, String field) {
        if (field.equals("-")) return 0;
        if (!field.matches("K?Q?k?q?") || field.isEmpty()) throw invalid("invalid castling rights");
        String ordered = "QKqk";
        int rights = 0;
        for (int index = 0; index < ordered.length(); index++) {
            if (field.indexOf(ordered.charAt(index)) < 0) continue;
            boolean white = index < 2;
            int row = white ? 7 : 0;
            Piece king = board.getPiece(new Position(row, 4));
            Piece rook = board.getPiece(new Position(row, index % 2 == 0 ? 0 : 7));
            if (!(king instanceof King) || !(rook instanceof Rook) || king.isWhite() != white
                    || rook.isWhite() != white) throw invalid("castling right without its king and rook");
            rights |= 1 << index;
        }
        return rights;
    }

    private Position parseEnPassant(Board board, String field, int halfmoves) {
        if (field.equals("-")) return null;
        if (!Utils.isValidInput(field) || field.charAt(1) != (white ? '6' : '3') || halfmoves != 0) {
            throw invalid("invalid en passant square or halfmove clock");
        }
        Position ep = Utils.ultConversion(field);
        int offset = white ? 1 : -1;
        Piece pawn = board.getPiece(new Position(ep.getRow() + offset, ep.getCol()));
        if (!(pawn instanceof Pawn) || pawn.isWhite() == white || !board.isEmpty(ep)
                || !board.isEmpty(new Position(ep.getRow() - offset, ep.getCol()))) {
            throw invalid("en passant square does not follow a pawn double step");
        }
        return ep;
    }

    private static int number(String field, int min, int max) {
        try {
            if (field.matches("[0-9]+")) {
                int value = Integer.parseInt(field);
                if (value >= min && value <= max) return value;
            }
        } catch (NumberFormatException ignored) {
            // Report overflow with the same message as any out-of-range counter.
        }
        throw invalid("counter out of range");
    }

    private static IllegalArgumentException invalid(String reason) {
        return new IllegalArgumentException("Invalid FEN: " + reason + ".");
    }
}
