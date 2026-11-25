package oopchesscode.game;

import javax.swing.*;
import java.awt.*;

/** Shared accessible controls; no game or session state. */
final class SwingControls {
    private SwingControls() {}
    static JButton button(String text, String description) {
        JButton button = new JButton(text);
        // Let the same UI delegate choose the foreground, background, border,
        // and focus treatment, including when the control is disabled.
        button.setToolTipText(description);
        button.getAccessibleContext().setAccessibleDescription(description);
        return button;
    }

    static void bind(JComponent component, int key, int modifiers, String name, Runnable action) {
        bind(component, JComponent.WHEN_IN_FOCUSED_WINDOW, KeyStroke.getKeyStroke(key, modifiers), name, action);
    }

    static void bind(JComponent component, int condition, KeyStroke key, String name, Runnable action) {
        component.getInputMap(condition).put(key, name);
        component.getActionMap().put(name, new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent event) { action.run(); }
        });
    }

    static JPanel row(JButton... buttons) {
        JPanel row = new JPanel(new GridLayout(1, buttons.length, 6, 0));
        row.setOpaque(false);
        for (JButton button : buttons) row.add(button);
        return row;
    }

}
