package oopchesscode.game;

import java.util.ArrayList;
import java.util.List;
import java.util.function.LongSupplier;

final class MatchClock {
    enum Control {
        UNTIMED("Untimed", 0), FIVE("5 minutes", 5), TEN("10 minutes", 10);

        final String label;
        final long nanos;

        Control(String label, int minutes) {
            this.label = label;
            nanos = minutes * 60_000_000_000L;
        }

        @Override public String toString() { return label; }
    }

    record Time(long white, long black) {
        long forSide(boolean whiteSide) { return whiteSide ? white : black; }

        Time charge(boolean whiteSide, long elapsed) {
            return whiteSide ? new Time(Math.max(0, white - elapsed), black)
                    : new Time(white, Math.max(0, black - elapsed));
        }
    }

    private final Control control;
    private final LongSupplier nanoTime;
    private final ArrayList<Time> positions;
    private Time remaining;
    private boolean running;
    private boolean whiteRunning;
    private long started;

    MatchClock(Control control, LongSupplier nanoTime) {
        this(control, nanoTime, List.of(new Time(control.nanos, control.nanos)),
                new Time(control.nanos, control.nanos));
    }

    MatchClock(Control control, LongSupplier nanoTime, List<Time> positions, Time remaining) {
        this.control = control;
        this.nanoTime = nanoTime;
        this.positions = new ArrayList<>(positions);
        this.remaining = remaining;
    }

    boolean running() { return running; }

    boolean timed() { return control != Control.UNTIMED; }

    Time reading() {
        return running ? remaining.charge(whiteRunning, Math.max(0, nanoTime.getAsLong() - started)) : remaining;
    }

    void resume(boolean white) {
        if (!timed() || running) return;
        whiteRunning = white;
        started = nanoTime.getAsLong();
        running = true;
    }

    void pause() {
        remaining = reading();
        running = false;
    }

    boolean expired(boolean white) { return timed() && reading().forSide(white) == 0; }

    void commitMove() {
        pause();
        if (timed()) positions.add(remaining);
    }

    void rewind(int ply) {
        if (!timed()) return;
        pause();
        remaining = positions.get(ply);
        positions.subList(ply + 1, positions.size()).clear();
    }

    Time atPly(int ply) { return timed() ? positions.get(ply) : remaining; }
    List<Time> checkpoints() { return List.copyOf(positions); }
}
