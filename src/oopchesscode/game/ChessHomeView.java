package oopchesscode.game;

import javax.swing.*;
import java.awt.*;
import static oopchesscode.game.VisualStyle.Role.*;
import static oopchesscode.game.VisualStyle.label;
import static oopchesscode.game.SwingControls.*;

/** Home presentation. The window owns all transitions and live-game state. */
final class ChessHomeView {
    record Actions(Runnable computer, Runnable local, Runnable practice, Runnable open, Runnable resume) {}
    private final JButton resume = button("Resume game", "Continue the current or recovered game");
    private final JLabel homeStatus = label("", 14, false, MUTED);
    private JButton playComputer;
    private final JPanel panel;

    ChessHomeView(Actions actions) { panel = build(actions); }
    JPanel panel() { return panel; }
    void update(boolean hasGame) {
        resume.setEnabled(hasGame);
        resume.setVisible(hasGame);
        homeStatus.setText(hasGame ? "Your game is paused and ready to resume."
                : "Start a game, open a PGN, or try a puzzle.");
    }
    void focus(boolean hasGame) { (hasGame ? resume : playComputer).requestFocusInWindow(); }

    private JPanel build(Actions actions) {
        JPanel holder = new JPanel(new GridBagLayout());
        holder.setOpaque(false);
        JPanel hero = VisualStyle.card(new BorderLayout(0, 12), STATUS, 24);
        JPanel headline = new JPanel(new GridLayout(2, 1, 0, 4));
        headline.setOpaque(false);
        headline.add(label("Chess at", 34, true, STATUS_TEXT));
        headline.add(label("your pace", 34, true, STATUS_TEXT));
        hero.add(headline, BorderLayout.NORTH);
        hero.add(new BoardMotif(), BorderLayout.CENTER);

        JPanel menu = new JPanel(new BorderLayout(0, 14));
        menu.setOpaque(false);
        JPanel heading = new JPanel(new GridLayout(2, 1, 0, 3));
        heading.setOpaque(false);
        heading.add(label("Choose how to play", 24, true, INK));
        heading.add(homeStatus);
        menu.add(heading, BorderLayout.NORTH);
        JPanel choices = new JPanel(new GridLayout(2, 2, 12, 12));
        choices.setOpaque(false);
        playComputer = welcomeAction("Play computer", "Choose a side, level and clock.", actions.computer());
        choices.add(modeCard(playComputer, "Choose a side, level and clock.", 'N'));
        choices.add(modeCard(welcomeAction("Local two-player", "Share the board on one device.", actions.local()),
                "Share the board on one device.", 'P'));
        choices.add(modeCard(welcomeAction("Practice puzzles", "Solve eight checkmate positions.", actions.practice()),
                "Solve eight checkmate positions.", 'Q'));
        choices.add(modeCard(welcomeAction("Open saved PGN…", "Continue a game or review moves.", actions.open()),
                "Continue a game or review moves.", 'R'));
        menu.add(choices, BorderLayout.CENTER);
        VisualStyle.role(resume, PRIMARY);
        resume.addActionListener(event -> actions.resume().run());
        menu.add(resume, BorderLayout.SOUTH);

        GridBagConstraints left = new GridBagConstraints();
        left.gridx = 0;
        left.weightx = .42;
        left.weighty = 1;
        left.fill = GridBagConstraints.BOTH;
        left.insets = new Insets(12, 0, 6, 22);
        holder.add(hero, left);
        GridBagConstraints right = new GridBagConstraints();
        right.gridx = 1;
        right.weightx = .58;
        right.weighty = 1;
        right.fill = GridBagConstraints.BOTH;
        right.insets = new Insets(12, 0, 6, 0);
        holder.add(menu, right);
        return holder;
    }

    private JPanel modeCard(JButton action, String description, char piece) {
        JPanel card = VisualStyle.card(new GridBagLayout(), CARD, 13);
        JPanel content = new JPanel(new BorderLayout(0, 8));
        content.setOpaque(false);
        JLabel art = label("", 13, false, MUTED);
        art.setIcon(new Icon() {
            @Override public int getIconWidth() { return 42; }
            @Override public int getIconHeight() { return 42; }
            @Override public void paintIcon(Component component, Graphics g, int x, int y) {
                PieceArt.paint((Graphics2D) g, x, y, 42, piece, true);
            }
        });
        content.add(art, BorderLayout.NORTH);
        JLabel summary = label("<html><body style='width:118px'>" + description + "</body></html>", 13, false, MUTED);
        content.add(summary, BorderLayout.CENTER);
        content.add(action, BorderLayout.SOUTH);
        GridBagConstraints fill = new GridBagConstraints();
        fill.weightx = 1;
        fill.fill = GridBagConstraints.HORIZONTAL;
        card.add(content, fill);

        var mouse = new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent event) {
                if (SwingUtilities.isLeftMouseButton(event)) action.doClick();
            }
            @Override public void mouseEntered(java.awt.event.MouseEvent event) { action.getModel().setRollover(true); }
            @Override public void mouseExited(java.awt.event.MouseEvent event) { action.getModel().setRollover(false); }
        };
        for (Component target : java.util.List.of(card, content, art, summary)) target.addMouseListener(mouse);
        return card;
    }

    private JButton welcomeAction(String text, String description, Runnable action) {
        JButton button = button(text, description);
        button.addActionListener(event -> action.run());
        return button;
    }

}
