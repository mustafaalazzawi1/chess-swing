package oopchesscode.game;

import oopchesscode.game.ChessMatch.Move;
import oopchesscode.game.ChessMatch.Result;
import oopchesscode.utils.Utils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Reads and writes one orthodox game using PGN main-line movetext. */
final class Pgn {
    static final int MAX_PLIES = 20_000;
    private static final String[] REQUIRED_TAGS = {"Event", "Site", "Date", "Round", "White", "Black", "Result"};
    private static final String CONCLUSION = "ChessSwingConclusion";
    private static final Pattern TAG = Pattern.compile("\\[\\s*([A-Za-z0-9_]+)\\s+\"((?:[^\"\\\\\\r\\n]|\\\\[\"\\\\])*)\"\\s*]");
    private static final Pattern NUMBER = Pattern.compile("([1-9][0-9]{0,6})(\\.{3}|\\.)");
    private static final Pattern SAN = Pattern.compile(
            "(?:O-O(?:-O)?|[KQRBN][a-h]?[1-8]?x?[a-h][1-8]|(?:[a-h]x)?[a-h][1-8](?:=[QRBN])?)[+#]?");

    record Game(StartPosition start, Map<String, String> tags, ChessMatch live) {
        Game {
            tags = Collections.unmodifiableMap(new LinkedHashMap<>(tags));
        }

        static Game fresh() {
            return new Game(StartPosition.STANDARD, Map.of(), new ChessMatch());
        }

        ChessMatch atPly(int ply) {
            List<Move> moves = live.history();
            if (ply < 0 || ply > moves.size()) throw new IllegalArgumentException("Invalid replay position.");
            ChessMatch replay = start.newMatch();
            for (int i = 0; i < ply; i++) {
                if (!replay.play(moves.get(i))) throw new IllegalStateException("The recorded move list is inconsistent.");
            }
            return replay;
        }

        Game takeBack() {
            if (live.history().isEmpty()) throw new IllegalStateException("There is no move to take back.");
            return takeBackTo(live.history().size() - 1);
        }

        Game takeBackTo(int ply) {
            if (ply >= live.history().size()) throw new IllegalArgumentException("Expected an earlier position.");
            Map<String, String> metadata = new LinkedHashMap<>(tags);
            metadata.remove("Termination");
            return new Game(start, metadata, atPly(ply));
        }

        String moveLabel(int ply) {
            return start.numberAt(ply) + (start.whiteAt(ply) ? "." : "…");
        }
    }

    private Pgn() {}

    static Game parse(String text) {
        Reader reader = new Reader(text);
        Map<String, String> tags = reader.tags();
        String setup = tags.get("SetUp");
        String fen = tags.get("FEN");
        if ((setup != null && !setup.equals("0") && !setup.equals("1"))
                || ("1".equals(setup) != (fen != null))) throw error("SetUp and FEN tags must agree.");
        String variant = tags.get("Variant");
        if (variant != null && !variant.equalsIgnoreCase("Standard") && !variant.equalsIgnoreCase("Chess")) {
            throw error("Only standard chess is supported.");
        }
        StartPosition start = StartPosition.parse(fen);
        ChessMatch match = start.newMatch();
        String token;
        String result = null;
        boolean numbered = false;
        int ply = 0;
        while ((token = reader.next()) != null) {
            if (result != null) throw error("Unexpected content after the result. Open one game at a time.");
            if (isResult(token)) {
                if (numbered) throw error("Expected a move after its move number.");
                result = token;
                continue;
            }
            Matcher number = NUMBER.matcher(token);
            if (number.matches()) {
                boolean whiteNumber = number.group(2).length() == 1;
                if (numbered || Integer.parseInt(number.group(1)) != start.numberAt(ply)
                        || whiteNumber != start.whiteAt(ply)) {
                    throw error("Unexpected move number " + token + "; expected "
                            + start.numberAt(ply) + (start.whiteAt(ply) ? "." : "...") + ".");
                }
                numbered = true;
                continue;
            }
            if (ply >= MAX_PLIES) throw error("The game exceeds " + MAX_PLIES + " half-moves.");
            readMove(match, token, start, ply++);
            numbered = false;
        }
        if (result == null) throw error("Missing result marker at the end of the game (use * for an unfinished game).");
        if (tags.containsKey("Result") && !tags.get("Result").equals(result)) {
            throw error("The Result tag disagrees with the final result marker.");
        }
        applyResult(match, result, tags.get(CONCLUSION));
        return new Game(start, tags, match);
    }

    private static void readMove(ChessMatch match, String token, StartPosition start, int ply) {
        String san = token.replaceFirst("[!?]+$", "").replace('0', 'O');
        String label = start.numberAt(ply) + (start.whiteAt(ply) ? "." : "...");
        String shown = token.length() > 40 ? token.substring(0, 40) + "…" : token;
        if (!SAN.matcher(san).matches()) throw error(label + " " + shown + " is not standard algebraic notation.");
        if (match.result() != Result.ACTIVE) throw error(label + " " + shown + " comes after the game ended.");
        String stem = san.replaceFirst("[+#]$", "");
        int end = stem.contains("=") ? stem.length() - 2 : stem.length();
        String destination = stem.equals("O-O") ? (match.whiteTurn() ? "g1" : "g8")
                : stem.equals("O-O-O") ? (match.whiteTurn() ? "c1" : "c8") : stem.substring(end - 2, end);
        for (Move move : match.legalMovesTo(Utils.ultConversion(destination))) {
            if (!match.sanStem(move).equals(stem)) continue;
            match.play(move);
            String actual = match.notationAt(ply);
            if (!san.equals(actual)) throw error(label + " " + shown + " should be " + actual + ".");
            return;
        }
        throw error(label + " " + shown + " is illegal or ambiguous in this position.");
    }

    private static boolean isResult(String token) {
        return switch (token) {
            case "*", "1-0", "0-1", "1/2-1/2" -> true;
            default -> false;
        };
    }

    private static void applyResult(ChessMatch match, String token, String conclusion) {
        if (match.result() != Result.ACTIVE) {
            if (conclusion != null || !match.resultToken().equals(token)) {
                throw error("The result conflicts with the final board position.");
            }
            return;
        }
        if (conclusion != null) {
            switch (conclusion) {
                case "resigned" -> match.resign();
                case "flagged" -> match.flagFall();
                case "claimed" -> {
                    if (!match.claimDraw()) throw error("A draw claim is invalid in the final position.");
                }
                default -> throw error("Unknown Chess Swing conclusion.");
            }
            if (!match.resultToken().equals(token)) throw error("The result conflicts with the recorded conclusion.");
        } else if (!token.equals("*")) {
            // PGN may record an external ending, such as a resignation or agreed draw.
            match.recordResult(token);
        }
    }

    static String write(Game game) {
        ChessMatch match = game.live();
        Map<String, String> tags = new LinkedHashMap<>();
        String[] fallback = {"Chess Swing", "Local", "????.??.??", "?", "White", "Black", "*"};
        for (int i = 0; i < REQUIRED_TAGS.length; i++) {
            String key = REQUIRED_TAGS[i];
            tags.put(key, game.tags().getOrDefault(key, fallback[i]));
        }
        tags.put("Result", match.resultToken());
        if (game.start().fen() != null) {
            tags.put("SetUp", "1");
            tags.put("FEN", game.start().fen());
        }
        if (match.result() == Result.RESIGNED) tags.put(CONCLUSION, "resigned");
        else if (match.result() == Result.TIME) tags.put(CONCLUSION, "flagged");
        else if (match.wasDrawClaimed()) tags.put(CONCLUSION, "claimed");
        game.tags().forEach((key, value) -> {
            if (!key.equals(CONCLUSION) && !key.equals("SetUp") && !key.equals("FEN")) tags.putIfAbsent(key, value);
        });
        StringBuilder text = new StringBuilder();
        tags.forEach((key, value) -> text.append('[').append(key).append(" \"")
                .append(value.replace("\\", "\\\\").replace("\"", "\\\"")).append("\"]\n"));
        text.append('\n');
        StringBuilder line = new StringBuilder();
        List<String> notation = match.notation();
        for (int ply = 0; ply < notation.size(); ply++) {
            String move = notation.get(ply);
            if (game.start().whiteAt(ply)) move = game.start().numberAt(ply) + ". " + move;
            else if (ply == 0) move = game.start().numberAt(ply) + "... " + move;
            append(text, line, move);
        }
        append(text, line, match.resultToken());
        return text.append(line).append('\n').toString();
    }

    private static void append(StringBuilder text, StringBuilder line, String token) {
        if (!line.isEmpty() && line.length() + token.length() + 1 > 78) {
            text.append(line).append('\n');
            line.setLength(0);
        }
        if (!line.isEmpty()) line.append(' ');
        line.append(token);
    }

    private static IllegalArgumentException error(String message) { return new IllegalArgumentException(message); }

    private static final class Reader {
        private final String text;
        private int index;

        Reader(String text) {
            this.text = text;
            if (text.startsWith("\uFEFF")) index++;
        }

        Map<String, String> tags() {
            Map<String, String> tags = new LinkedHashMap<>();
            skipTrivia(true);
            while (peek('[')) {
                int tagLimit = Math.min(index + 1024, text.length());
                Matcher tag = TAG.matcher(text).region(index, tagLimit);
                if (!tag.lookingAt()) throw error("Malformed or overlong PGN tag near character " + (index + 1) + ".");
                String value = tag.group(2).replace("\\\"", "\"").replace("\\\\", "\\");
                if (tags.putIfAbsent(tag.group(1), value) != null) throw error("Duplicate tag " + tag.group(1) + ".");
                index = tag.end();
                skipTrivia(true);
            }
            return tags;
        }

        String next() {
            skipTrivia(true);
            if (index == text.length()) return null;
            Matcher number = NUMBER.matcher(text).region(index, text.length());
            if (number.lookingAt()) {
                index = number.end();
                return number.group();
            }
            int start = index;
            while (index < text.length() && !Character.isWhitespace(text.charAt(index))
                    && "{};()[]$".indexOf(text.charAt(index)) < 0) index++;
            if (start == index) throw error("Unexpected PGN character at " + (index + 1) + ".");
            return text.substring(start, index);
        }

        private void skipTrivia(boolean variations) {
            while (index < text.length()) {
                char c = text.charAt(index);
                if (Character.isWhitespace(c)) index++;
                else if (c == '{') {
                    int end = text.indexOf('}', index + 1);
                    if (end < 0) throw error("Unclosed PGN comment.");
                    index = end + 1;
                } else if (c == ';' || (c == '%' && (index == 0 || index == 1 && text.charAt(0) == '\uFEFF'
                        || text.charAt(index - 1) == '\n' || text.charAt(index - 1) == '\r'))) {
                    while (index < text.length() && text.charAt(index) != '\n' && text.charAt(index) != '\r') index++;
                } else if (c == '$' && variations) {
                    int start = ++index;
                    while (index < text.length() && Character.isDigit(text.charAt(index))) index++;
                    if (start == index) throw error("Missing numeric annotation after $.");
                } else if (c == '(' && variations) {
                    skipVariation();
                } else return;
            }
        }

        private void skipVariation() {
            int depth = 0;
            do {
                if (peek('(')) {
                    index++;
                    depth++;
                } else if (peek(')')) {
                    index++;
                    depth--;
                } else {
                    int start = index;
                    skipTrivia(false);
                    if (start == index) index++;
                }
            } while (depth > 0 && index < text.length());
            if (depth != 0) throw error("Unclosed PGN variation.");
        }

        private boolean peek(char c) { return index < text.length() && text.charAt(index) == c; }
    }
}
