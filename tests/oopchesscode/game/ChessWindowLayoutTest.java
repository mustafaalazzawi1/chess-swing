package oopchesscode.game;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;

/** Run with a graphical display after ./build.sh --test. */
public final class ChessWindowLayoutTest {
    private static final Set<String> HOME = Set.of("New game…", "Home", "Play computer", "Local two-player",
            "Practice puzzles", "Open saved PGN…");
    private static final Set<String> GAME = Set.of("New game…", "Home", "Start", "Back", "Next", "Live",
            "Takeback…", "Claim draw", "Resign…", "Hint");
    private static final Set<String> PRACTICE = Set.of("New game…", "Home", "Reset puzzle", "Next puzzle",
            "Show solution…", "Back to game");

    public static void main(String[] args) throws Exception {
        if (GraphicsEnvironment.isHeadless()) throw new IllegalStateException("A display is required.");
        Path profile = Files.createTempDirectory("chess-layout-");
        System.setProperty("chessswing.recovery", profile.resolve("recovery.pgn").toString());
        try {
            SwingUtilities.invokeAndWait(() -> {
                VisualStyle.install(VisualStyle.Appearance.LIGHT);
                checkWindow();
                VisualStyle.install(VisualStyle.Appearance.LIGHT);
                Font buttonFont = UIManager.getFont("Button.font");
                Font labelFont = UIManager.getFont("Label.font");
                UIManager.put("Button.font", buttonFont.deriveFont(18f));
                UIManager.put("Label.font", labelFont.deriveFont(18f));
                try {
                    checkWindow();
                } finally {
                    UIManager.put("Button.font", null);
                    UIManager.put("Label.font", null);
                }
            });
        } finally {
            Files.deleteIfExists(profile.resolve("recovery.pgn"));
            Files.deleteIfExists(profile.resolve("recovery.pgn.lock"));
            Files.deleteIfExists(profile);
        }
    }

    private static void checkWindow() {
        new ChessWindow().show();
        JFrame frame = null;
        for (Frame candidate : Frame.getFrames()) {
            if (candidate.isShowing() && candidate instanceof JFrame window) frame = window;
        }
        if (frame == null) throw new AssertionError("Chess window did not open.");
        try {
            Dimension initial = frame.getSize();
            checkSizes(frame, initial, HOME);
            require(findButton(frame.getContentPane(), "Resume game") == null, "Resume without a game");
            Timer accept = new Timer(25, event -> {
                for (Window window : Window.getWindows()) {
                    if (window instanceof JDialog dialog && dialog.isShowing() && "New game setup".equals(dialog.getTitle())) {
                        JButton start = findButton(dialog, "Start game");
                        require(start != null && start.getWidth() >= start.getPreferredSize().width, "Setup Start game label");
                        require(findButton(dialog, "Cancel") != null, "Setup Cancel label");
                        start.doClick(0);
                    }
                }
            });
            accept.start();
            try {
                findButton(frame.getContentPane(), "Local two-player").doClick(0);
            } finally {
                accept.stop();
            }
            checkSizes(frame, initial, GAME);
            require(findButton(frame.getContentPane(), "Hint").isEnabled(), "Human can request hint");
            findButton(frame.getContentPane(), "Home").doClick(0);
            require(findButton(frame.getContentPane(), "Resume game").isEnabled(), "Game can be resumed");
            findButton(frame.getContentPane(), "Practice puzzles").doClick(0);
            checkSizes(frame, initial, PRACTICE);
            findButton(frame.getContentPane(), "Back to game").doClick(0);
            require(findButton(frame.getContentPane(), "Hint").isShowing(), "Game controls restored");

            System.out.println("PASS layout: " + UIManager.getLookAndFeel().getName()
                    + ", " + UIManager.getFont("Button.font").getSize() + " pt, default and "
                    + frame.getMinimumSize().width + "×" + frame.getMinimumSize().height + ", light, dark and high contrast");
        } finally {
            frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
        }
    }

    private static void checkSizes(JFrame frame, Dimension initial, Set<String> labels) {
        JMenu view = frame.getJMenuBar().getMenu(1);
        for (int index : new int[]{2, 3, 1}) {
            view.getItem(index).doClick(0);
            for (Dimension size : new Dimension[]{initial, frame.getMinimumSize()}) {
                Dimension minimum = frame.getMinimumSize();
                frame.setSize(Math.max(size.width, minimum.width), Math.max(size.height, minimum.height));
                frame.validate();
                Set<String> observed = new LinkedHashSet<>();
                checkTree(frame.getContentPane(), observed);
                require(observed.equals(labels), "Unexpected controls: " + observed);
                JButton home = findButton(frame.getContentPane(), "Home");
                require(home.getForeground().equals(VisualStyle.palette().ink()), "Stale theme text color");
                require(home.getBackground().equals(VisualStyle.palette().surface()), "Stale theme button fill");
            }
        }
        view.getItem(2).doClick(0);
    }

    private static JButton findButton(Container parent, String name) {
        for (Component child : parent.getComponents()) {
            if (!child.isVisible()) continue;
            if (child instanceof JButton button && name.equals(button.getText())) return button;
            if (child instanceof Container nested) {
                JButton button = findButton(nested, name);
                if (button != null) return button;
            }
        }
        return null;
    }

    private static void checkTree(Container parent, Set<String> observed) {
        for (Component child : parent.getComponents()) {
            if (!child.isVisible()) continue;
            if (child instanceof JButton button && button.getText() != null && !button.getText().isBlank()) {
                String label = button.getText();
                observed.add(label);
                require(button.isFocusable(), "Missing keyboard focus: " + label);
                require(label.equals(button.getAccessibleContext().getAccessibleName()), "Accessible label: " + label);
                require(button.getWidth() >= button.getPreferredSize().width
                        && button.getHeight() >= button.getPreferredSize().height, "Preferred size clipped: " + label);
                Insets insets = button.getInsets();
                Rectangle view = new Rectangle(insets.left, insets.top,
                        button.getWidth() - insets.left - insets.right,
                        button.getHeight() - insets.top - insets.bottom);
                String fitted = SwingUtilities.layoutCompoundLabel(button, button.getFontMetrics(button.getFont()),
                        label, button.getIcon(), button.getVerticalAlignment(), button.getHorizontalAlignment(),
                        button.getVerticalTextPosition(), button.getHorizontalTextPosition(),
                        view, new Rectangle(), new Rectangle(), button.getIconTextGap());
                require(label.equals(fitted), "Truncated label: " + fitted);
            }
            if (child instanceof Container container) checkTree(container, observed);
        }
    }

    private static void require(boolean condition, String reason) {
        if (!condition) throw new AssertionError(reason);
    }
}
