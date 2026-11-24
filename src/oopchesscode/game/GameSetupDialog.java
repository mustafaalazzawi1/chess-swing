package oopchesscode.game;

import oopchesscode.game.ComputerSearch.Level;
import oopchesscode.game.MatchClock.Control;
import oopchesscode.game.MatchSession.Opponent;
import oopchesscode.game.MatchSession.Setup;

import javax.swing.*;
import java.awt.*;

final class GameSetupDialog {
    private GameSetupDialog() {}

    static Setup choose(JFrame owner, Setup current) {
        JComboBox<Opponent> opponent = new JComboBox<>(Opponent.values());
        JComboBox<String> side = new JComboBox<>(new String[]{"White", "Black"});
        JComboBox<Level> difficulty = new JComboBox<>(Level.values());
        JComboBox<Control> clock = new JComboBox<>(Control.values());
        opponent.setSelectedItem(current.opponent());
        side.setSelectedIndex(current.humanWhite() ? 0 : 1);
        difficulty.setSelectedItem(current.level());
        clock.setSelectedItem(current.control());

        JPanel form = VisualStyle.card(new GridBagLayout(), VisualStyle.Role.CARD, 14);
        GridBagConstraints heading = new GridBagConstraints();
        heading.gridx = 0;
        heading.gridy = 0;
        heading.gridwidth = 2;
        heading.anchor = GridBagConstraints.WEST;
        heading.insets = new Insets(2, 4, 12, 4);
        form.add(VisualStyle.label("Set up your game", 21, true, VisualStyle.Role.INK), heading);
        addRow(form, 1, "Opponent", opponent);
        addRow(form, 2, "Your side", side);
        addRow(form, 3, "Difficulty", difficulty);
        addRow(form, 4, "Time per side", clock);
        JLabel detail = VisualStyle.label("", 13, false, VisualStyle.Role.MUTED);
        GridBagConstraints help = new GridBagConstraints();
        help.gridy = 5;
        help.gridwidth = 2;
        help.fill = GridBagConstraints.HORIZONTAL;
        help.insets = new Insets(6, 4, 0, 4);
        form.add(detail, help);
        Runnable refresh = () -> {
            boolean computer = opponent.getSelectedItem() == Opponent.COMPUTER;
            side.setEnabled(computer);
            difficulty.setEnabled(computer);
            Level level = (Level) difficulty.getSelectedItem();
            String message = computer ? level.depth + " half-move" + (level.depth == 1 ? "" : "s")
                    + " max · " + level.millis + " ms search budget"
                    : "Two people share the board.";
            detail.setText("<html><body style='width:258px'>" + message
                    + "<br>Timed games have no increment.</body></html>");
        };
        opponent.addActionListener(event -> refresh.run());
        difficulty.addActionListener(event -> refresh.run());
        refresh.run();
        Object[] options = {"Start game", "Cancel"};
        int choice = JOptionPane.showOptionDialog(owner, form, "New game setup", JOptionPane.DEFAULT_OPTION,
                JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
        return choice == 0 ? new Setup((Opponent) opponent.getSelectedItem(), side.getSelectedIndex() == 0,
                (Level) difficulty.getSelectedItem(), (Control) clock.getSelectedItem()) : null;
    }

    private static void addRow(JPanel form, int row, String text, JComponent field) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = row;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(3, 4, 3, 4);
        JLabel label = new JLabel(text);
        label.setLabelFor(field);
        form.add(label, c);
        c.gridx = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        form.add(field, c);
    }
}
