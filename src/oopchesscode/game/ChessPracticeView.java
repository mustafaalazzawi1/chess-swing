package oopchesscode.game;

import javax.swing.*;
import java.awt.*;
import static oopchesscode.game.VisualStyle.Role.*;
import static oopchesscode.game.VisualStyle.label;
import static oopchesscode.game.SwingControls.*;

import javax.swing.border.EmptyBorder;

/** Practice controls and feedback, independent of live-game state. */
final class ChessPracticeView {
    record Actions(Runnable reset, Runnable next, Runnable solution, Runnable back) {}
    private final JLabel puzzleTitle = label("", 20, true, INK);
    private final JLabel puzzleNumber = label("", 12, true, MUTED);
    private final JLabel puzzleGoal = label("", 15, true, INK);
    private final JTextArea puzzleFeedback = new JTextArea(3, 20);
    private JButton backToGame;
    private final JPanel panel;

    ChessPracticeView(Actions actions) { panel = build(actions); }
    JPanel panel() { return panel; }
    void feedback(String text) { puzzleFeedback.setText(text); }
    void update(PuzzleSession practice, boolean hasGame) {
        backToGame.setText(hasGame ? "Back to game" : "Back to home");
        puzzleTitle.setText(practice.puzzle().title());
        puzzleNumber.setText("PRACTICE  ·  " + practice.counter());
        puzzleGoal.setText("Checkmate in " + practice.puzzle().mateIn());
        puzzleFeedback.setText(practice.feedback());
    }

    private JPanel build(Actions actions) {
        JPanel panel = VisualStyle.card(new BorderLayout(0, 16), CARD, 14);
        JPanel heading = VisualStyle.card(new GridLayout(3, 1, 0, 7), STATUS, 14);
        VisualStyle.role(puzzleNumber, STATUS_MUTED);
        VisualStyle.role(puzzleTitle, STATUS_TEXT);
        VisualStyle.role(puzzleGoal, STATUS_TEXT);
        heading.add(puzzleNumber);
        heading.add(puzzleTitle);
        heading.add(puzzleGoal);
        panel.add(heading, BorderLayout.NORTH);
        puzzleFeedback.setEditable(false);
        puzzleFeedback.setFocusable(false);
        puzzleFeedback.setLineWrap(true);
        puzzleFeedback.setWrapStyleWord(true);
        puzzleFeedback.setFont(puzzleFeedback.getFont().deriveFont(Math.max(14f, UIManager.getFont("Label.font").getSize2D())));
        VisualStyle.role(puzzleFeedback, SURFACE);
        puzzleFeedback.setBorder(new EmptyBorder(3, 6, 3, 6));
        puzzleFeedback.getAccessibleContext().setAccessibleName("Puzzle feedback");
        panel.add(puzzleFeedback, BorderLayout.CENTER);

        JButton reset = button("Reset puzzle", "Restore the original puzzle position");
        reset.addActionListener(event -> actions.reset().run());
        JButton nextPuzzle = button("Next puzzle", "Go to the next exercise");
        nextPuzzle.addActionListener(event -> actions.next().run());
        JButton solution = button("Show solution…", "Explain the idea and list accepted moves");
        solution.addActionListener(event -> actions.solution().run());
        backToGame = button("Back to game", "Resume the live or recovered game, or return home");
        backToGame.addActionListener(event -> actions.back().run());
        JPanel controls = new JPanel(new GridLayout(2, 1, 0, 6));
        controls.setOpaque(false);
        VisualStyle.role(nextPuzzle, PRIMARY);
        controls.add(row(reset, nextPuzzle));
        controls.add(row(solution, backToGame));
        panel.add(controls, BorderLayout.SOUTH);
        return panel;
    }

}
