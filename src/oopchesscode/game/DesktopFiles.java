package oopchesscode.game;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/** Handles file chooser, explicit saves, and the independent local recovery copy. */
final class DesktopFiles {
    private final JFrame owner;
    private final JLabel indicator;
    private final Path recovery = Path.of(System.getProperty("chessswing.recovery",
            Path.of(System.getProperty("user.home"), ".chess-swing", "recovery.pgn").toString()))
            .toAbsolutePath().normalize();
    // Keep the channel open for the lifetime of the window so another instance cannot overwrite its recovery.
    private FileChannel recoveryLockChannel;
    private boolean recoveryBlocked;
    private boolean dirty;

    DesktopFiles(JFrame owner, JLabel indicator) {
        this.owner = owner;
        this.indicator = indicator;
    }

    MatchSession recover() {
        try {
            lockRecovery();
        } catch (IOException | SecurityException | OverlappingFileLockException error) {
            close();
            recoveryBlocked = true;
            indicator.setText("Recovery unavailable");
            indicator.setToolTipText(detail(error));
            return null;
        }
        try {
            if (Files.notExists(recovery, LinkOption.NOFOLLOW_LINKS)) {
                indicator.setText("Local recovery ready");
                return null;
            }
            if (Files.isSymbolicLink(recovery)) throw new IOException("The recovery copy is a symbolic link.");
            MatchSession recovered = SessionRecovery.decode(PgnFiles.read(recovery), System::nanoTime);
            Pgn.Game restored = recovered.game();
            if (recovered.setup.equals(MatchSession.Setup.DEFAULT) && restored.live().history().isEmpty() && restored.live().result() == ChessMatch.Result.ACTIVE
                    && restored.start().equals(StartPosition.STANDARD) && "Chess Swing".equals(restored.tags().get("Event"))) {
                indicator.setText("Local recovery ready");
                return null;
            }
            dirty = true;
            indicator.setText("Recovered game ready to resume");
            return recovered;
        } catch (IOException | IllegalArgumentException | SecurityException error) {
            recoveryBlocked = true;
            indicator.setText("Recovery unavailable");
            indicator.setToolTipText(recovery.toString());
            message("Recovery copy kept", "The recovery copy was left in place. Automatic recovery is disabled "
                    + "for this session.\n" + detail(error));
        }
        return null;
    }

    private void lockRecovery() throws IOException {
        Files.createDirectories(recovery.getParent());
        Path lock = recovery.resolveSibling(recovery.getFileName() + ".lock");
        recoveryLockChannel = FileChannel.open(lock, StandardOpenOption.CREATE,
                StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS);
        if (recoveryLockChannel.tryLock() == null) throw new IOException("Another Chess Swing window owns the recovery copy.");
    }

    void close() {
        if (recoveryLockChannel == null) return;
        try {
            recoveryLockChannel.close();
        } catch (IOException error) {
            indicator.setToolTipText(detail(error));
        } finally {
            recoveryLockChannel = null;
        }
    }

    void autosave(MatchSession session, boolean changed) {
        dirty = changed;
        checkpoint(session);
    }

    void checkpoint(MatchSession session) {
        if (recoveryBlocked) return;
        try {
            if (Files.isSymbolicLink(recovery)) throw new IOException("The recovery copy is a symbolic link.");
            MatchClock.Time reading = session.clock.reading();
            if (session.clock.timed() && session.live().result() == ChessMatch.Result.ACTIVE
                    && reading.forSide(session.live().whiteTurn()) == 0) return;
            PgnFiles.write(recovery, SessionRecovery.encode(session, reading));
            indicator.setText("Recovery copy updated");
            indicator.setToolTipText(recovery.toString());
        } catch (IOException | SecurityException error) {
            indicator.setText("Recovery write failed");
            indicator.setToolTipText(detail(error));
        }
    }

    boolean allowReplace(Pgn.Game current, String operation, boolean exiting) {
        if (!dirty) return true;
        Object[] choices = {"Save PGN…", exiting ? "Exit without exporting" : "Discard", "Cancel"};
        String recoveryNote = exiting && !recoveryBlocked && !indicator.getText().equals("Recovery write failed")
                ? "\nYour local recovery copy remains available." : "";
        int choice = JOptionPane.showOptionDialog(owner,
                "This game has changes since the last PGN save.\nSave them before you " + operation + "?" + recoveryNote,
                "Unsaved game", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, choices, choices[0]);
        return choice == 1 || choice == 0 && save(current);
    }

    boolean save(Pgn.Game game) {
        JFileChooser chooser = chooser("Save game as PGN");
        chooser.setSelectedFile(new java.io.File("chess-game.pgn"));
        if (chooser.showSaveDialog(owner) != JFileChooser.APPROVE_OPTION) return false;
        Path path = chooser.getSelectedFile().toPath().toAbsolutePath().normalize();
        if (!path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".pgn")) {
            path = path.resolveSibling(path.getFileName() + ".pgn");
        }
        try {
            if (isRecovery(path)) {
                message("Choose another destination", "That path belongs to the automatic recovery copy.");
                return false;
            }
            if (Files.exists(path, LinkOption.NOFOLLOW_LINKS)
                    && JOptionPane.showConfirmDialog(owner, "Replace " + path.getFileName() + "?",
                    "Overwrite PGN", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) {
                return false;
            }
            PgnFiles.write(path, game);
            dirty = false;
            if (!indicator.getText().equals("Recovery write failed") && !recoveryBlocked) {
                indicator.setText("PGN saved");
                indicator.setToolTipText(path.toString());
            }
            return true;
        } catch (IOException | SecurityException error) {
            message("Could not save PGN", detail(error));
            return false;
        }
    }

    private boolean isRecovery(Path path) throws IOException {
        if (path.equals(recovery) || Files.exists(path) && Files.exists(recovery) && Files.isSameFile(path, recovery)) {
            return true;
        }
        return path.getFileName().equals(recovery.getFileName())
                && Files.exists(path.getParent()) && Files.exists(recovery.getParent())
                && Files.isSameFile(path.getParent(), recovery.getParent());
    }

    void open(Consumer<Pgn.Game> accept, Consumer<Boolean> setBusy) {
        JFileChooser chooser = chooser("Open PGN game");
        if (chooser.showOpenDialog(owner) != JFileChooser.APPROVE_OPTION) return;
        Path path = chooser.getSelectedFile().toPath();
        setBusy.accept(true);
        new SwingWorker<Pgn.Game, Void>() {
            @Override protected Pgn.Game doInBackground() throws IOException {
                return PgnFiles.read(path);
            }

            @Override protected void done() {
                try {
                    accept.accept(get());
                } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                    message("PGN open interrupted", "The current game is still open.");
                } catch (ExecutionException error) {
                    message("Could not open PGN", detail(error.getCause()) + "\nThe current game is still open.");
                } finally {
                    setBusy.accept(false);
                }
            }
        }.execute();
    }

    private static JFileChooser chooser(String title) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(title);
        chooser.setFileFilter(new FileNameExtensionFilter("Portable Game Notation (*.pgn)", "pgn"));
        return chooser;
    }

    private void message(String title, String body) {
        JTextArea content = new JTextArea(body, 4, 36);
        content.setEditable(false);
        content.setLineWrap(true);
        content.setWrapStyleWord(true);
        content.setOpaque(false);
        JOptionPane.showMessageDialog(owner, new JScrollPane(content), title, JOptionPane.ERROR_MESSAGE);
    }

    private static String detail(Throwable error) {
        String message = error.getMessage();
        return message == null || message.isBlank() ? error.getClass().getSimpleName() : message;
    }
}
