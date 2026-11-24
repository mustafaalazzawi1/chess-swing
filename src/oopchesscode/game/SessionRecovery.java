package oopchesscode.game;

import oopchesscode.game.ChessMatch.Result;
import oopchesscode.game.ComputerSearch.Level;
import oopchesscode.game.MatchClock.Control;
import oopchesscode.game.MatchClock.Time;
import oopchesscode.game.MatchSession.Opponent;
import oopchesscode.game.MatchSession.Setup;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.LongSupplier;

/** Adapts the existing atomic PGN recovery file with private, versioned session tags. */
final class SessionRecovery {
    private static final String PREFIX = "ChessSwingLocal";
    private static final String SESSION = PREFIX + "Session";
    private static final String CLOCK = PREFIX + "Clock";
    private static final int CHUNK = 32;

    private SessionRecovery() {}

    static Pgn.Game encode(MatchSession session) {
        return encode(session, session.clock.reading());
    }

    static Pgn.Game encode(MatchSession session, Time reading) {
        Pgn.Game game = session.exportGame();
        if (session.setup.equals(Setup.DEFAULT)) return game;
        Map<String, String> tags = new LinkedHashMap<>(game.tags());
        Setup setup = session.setup;
        tags.put(SESSION, String.join(" ", "1", setup.opponent().name(), setup.humanWhite() ? "w" : "b",
                setup.level().name(), setup.control().name(), encodeTime(reading)));
        if (session.clock.timed()) {
            List<Time> times = session.clock.checkpoints();
            for (int i = 0; i < times.size(); i += CHUNK) {
                StringBuilder values = new StringBuilder();
                for (Time time : times.subList(i, Math.min(i + CHUNK, times.size()))) {
                    if (!values.isEmpty()) values.append(' ');
                    values.append(encodeTime(time));
                }
                tags.put(CLOCK + i / CHUNK, values.toString());
            }
        }
        return new Pgn.Game(game.start(), tags, game.live());
    }

    static MatchSession decode(Pgn.Game saved, LongSupplier now) {
        Pgn.Game game = clean(saved);
        String data = saved.tags().get(SESSION);
        if (data == null) return MatchSession.imported(game, now);
        String[] fields = data.split(" ", -1);
        try {
            if (fields.length != 6 || !fields[0].equals("1") || !fields[2].matches("[wb]")) throw invalid();
            Control control = Control.valueOf(fields[4]);
            Setup setup = new Setup(Opponent.valueOf(fields[1]), fields[2].equals("w"), Level.valueOf(fields[3]), control);
            Time time = decodeTime(fields[5], control);
            ArrayList<Time> snapshots = new ArrayList<>();
            long chunks = saved.tags().keySet().stream().filter(key -> key.startsWith(CLOCK)).count();
            if (control != Control.UNTIMED) {
                int count = game.live().history().size() + 1;
                if (chunks != (count + CHUNK - 1) / CHUNK) throw invalid();
                for (int i = 0; i < chunks; i++) {
                    String chunk = saved.tags().get(CLOCK + i);
                    if (chunk == null) throw invalid();
                    String[] entries = chunk.split(" ", -1);
                    if (entries.length != Math.min(CHUNK, count - i * CHUNK)) throw invalid();
                    for (String entry : entries) snapshots.add(decodeTime(entry, control));
                }
                validateClock(game, control, snapshots, time);
            } else if (chunks != 0) {
                throw invalid();
            } else {
                snapshots.add(time);
            }
            return new MatchSession(game, setup, new MatchClock(control, now, snapshots, time));
        } catch (IllegalArgumentException error) {
            throw invalid();
        }
    }

    static Pgn.Game clean(Pgn.Game game) {
        Map<String, String> tags = new LinkedHashMap<>(game.tags());
        tags.keySet().removeIf(key -> key.startsWith(PREFIX));
        return new Pgn.Game(game.start(), tags, game.live());
    }

    private static void validateClock(Pgn.Game game, Control control, List<Time> times, Time current) {
        Time previous = new Time(control.nanos, control.nanos);
        if (!times.getFirst().equals(previous)) throw invalid();
        for (int i = 1; i < times.size(); i++) {
            Time entry = times.get(i);
            boolean mover = game.start().whiteAt(i - 1);
            if (!validTransition(previous, entry, mover) || entry.forSide(mover) == 0) throw invalid();
            previous = entry;
        }
        boolean turn = game.live().whiteTurn();
        if (!validTransition(previous, current, turn)
                || (current.forSide(turn) == 0) != (game.live().result() == Result.TIME)) throw invalid();
    }

    private static boolean validTransition(Time from, Time to, boolean side) {
        return from.forSide(!side) == to.forSide(!side) && from.forSide(side) >= to.forSide(side);
    }

    private static String encodeTime(Time time) {
        return Long.toString(time.white(), 36) + ":" + Long.toString(time.black(), 36);
    }

    private static Time decodeTime(String value, Control control) {
        String[] fields = value.split(":", -1);
        if (fields.length != 2 || !fields[0].matches("[0-9a-z]{1,9}") || !fields[1].matches("[0-9a-z]{1,9}")) {
            throw invalid();
        }
        Time time = new Time(Long.parseLong(fields[0], 36), Long.parseLong(fields[1], 36));
        if (time.white() > control.nanos || time.black() > control.nanos) throw invalid();
        return time;
    }

    private static IllegalArgumentException invalid() {
        return new IllegalArgumentException("The local session settings or clock checkpoints are invalid.");
    }
}
