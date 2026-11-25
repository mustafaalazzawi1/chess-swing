package oopchesscode.game;

import oopchesscode.game.ChessMatch.Move;
import oopchesscode.position.Position;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/** View contracts: inputs are positions; emitted actions never mutate the engine. */
public final class ChessViewTest {
    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            VisualStyle.install(VisualStyle.Appearance.LIGHT);
            List<Position> selected = new ArrayList<>();
            ChessBoardView board = new ChessBoardView(selected::add);
            ChessMatch match = new ChessMatch();
            Position e2 = new Position(6, 4);
            Position e4 = new Position(4, 4);
            Move suggestion = new Move(e2, e4, null);
            board.render(match, e2, match.legalMovesFrom(e2), suggestion, false);
            JButton pawn = square(board.panel(), "e2, white pawn");
            require(pawn.getAccessibleContext().getAccessibleName().contains("selected"), "Selection announced");
            JButton target = square(board.panel(), "e4, empty");
            require(target.getAccessibleContext().getAccessibleName().contains("legal destination"), "Legal target announced");
            require(target.getAccessibleContext().getAccessibleName().contains("engine suggestion"), "Hint announced");
            target.doClick(0);
            require(selected.equals(List.of(e4)), "Callback uses board coordinates");
            require(match.history().isEmpty(), "View must not apply moves");
            board.render(match, null, List.of(), null, true);
            JButton corner = buttons(board.panel()).getFirst();
            require(corner.getAccessibleContext().getAccessibleName().startsWith("h1, white rook"), "Flipped orientation");
            corner.doClick(0);
            require(selected.getLast().equals(new Position(7,7)), "Flipped click uses logical square");
            require(!square(board.panel(), "e4, empty").getAccessibleContext().getAccessibleName().contains("suggestion"), "Stale hint removed");
            board.panel().setSize(480, 480);
            layout(board.panel());
            BufferedImage canvas = new BufferedImage(480, 480, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = canvas.createGraphics();
            try { board.panel().paint(graphics); } finally { graphics.dispose(); }
            System.out.println("PASS board view: orientation, selection, hints, callbacks and rendering");

            List<String> actions = new ArrayList<>();
            ChessHomeView home = new ChessHomeView(new ChessHomeView.Actions(
                    () -> actions.add("computer"), () -> actions.add("local"),
                    () -> actions.add("practice"), () -> actions.add("open"), () -> actions.add("resume")));
            home.update(false);
            require(!button(home.panel(), "Resume game").isVisible(), "Resume hidden without game");
            for (String text : List.of("Play computer", "Local two-player", "Practice puzzles", "Open saved PGN…"))
                button(home.panel(), text).doClick(0);
            home.update(true);
            require(button(home.panel(), "Resume game").isVisible(), "Resume shown with game");
            button(home.panel(), "Resume game").doClick(0);
            require(actions.equals(List.of("computer","local","practice","open","resume")), "Home intent callbacks");
            System.out.println("PASS home view: mode actions and resume state");

            actions.clear();
            ChessPracticeView practice = new ChessPracticeView(new ChessPracticeView.Actions(
                    () -> actions.add("reset"), () -> actions.add("next"),
                    () -> actions.add("solution"), () -> actions.add("back")));
            practice.update(new PuzzleSession(), false);
            for (String text : List.of("Reset puzzle", "Next puzzle", "Show solution…", "Back to home"))
                button(practice.panel(), text).doClick(0);
            require(actions.equals(List.of("reset","next","solution","back")), "Practice intent callbacks");
            practice.update(new PuzzleSession(), true);
            require(button(practice.panel(), "Back to game") != null, "Practice return label");
            System.out.println("PASS practice view: callbacks and return state");
        });
    }

    private static void layout(Container parent) {
        parent.doLayout();
        for (Component child : parent.getComponents()) if (child instanceof Container c) layout(c);
    }
    private static List<JButton> buttons(Container parent) {
        List<JButton> result = new ArrayList<>();
        for (Component child : parent.getComponents()) {
            if (child instanceof JButton button) result.add(button);
            else if (child instanceof Container container) result.addAll(buttons(container));
        }
        return result;
    }
    private static JButton button(Container root, String name) {
        return buttons(root).stream().filter(b -> name.equals(b.getText())).findFirst().orElseThrow();
    }
    private static JButton square(Container root, String name) {
        return buttons(root).stream().filter(b -> b.getAccessibleContext().getAccessibleName().startsWith(name)).findFirst().orElseThrow();
    }
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
