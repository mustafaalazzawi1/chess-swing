package oopchesscode.game;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.ColorUIResource;
import java.awt.*;

final class VisualStyle {
    enum Appearance { LIGHT, DARK, HIGH_CONTRAST }
    enum Role { CANVAS, SURFACE, INK, MUTED, STATUS, STATUS_TEXT, STATUS_MUTED, PRIMARY, DANGER, CARD, BOARD_FRAME }

    record Colors(Color canvas, Color surface, Color inset, Color line, Color ink, Color muted,
                  Color forest, Color inverse, Color inverseMuted, Color action, Color hover,
                  Color danger, Color boardFrame, Color lightSquare, Color darkSquare,
                  Color lastLight, Color lastDark, Color selection) {}

    private static final Colors LIGHT = colors(
            0xf3f1e8, 0xfffdf6, 0xefeee4, 0xd9ddd0, 0x172e25, 0x52665a,
            0x18372c, 0xfffcf1, 0xc5d9c9, 0x245943, 0x326d53, 0x8a3931, 0x263d31,
            0xe5dfc9, 0x829e86, 0xe7d49c, 0xb5ad70, 0xf0c76e);
    private static final Colors DARK = colors(
            0x15231e, 0x1c2e26, 0x24392e, 0x385347, 0xf1eee0, 0xb4c5b9,
            0x254538, 0xfffcf1, 0xd0dfd0, 0x2f6049, 0x3e735a, 0xffb6aa, 0x0a1712,
            0xc9c7aa, 0x567762, 0xdad199, 0x929055, 0xf0c76e);
    private static final Colors HIGH = colors(
            0x101721, 0x182333, 0x243449, 0x647995, 0xffffff, 0xd8e1ed,
            0x283c59, 0xffffff, 0xe6eefb, 0x285086, 0x35639e, 0xffb6aa, 0x070d18,
            0xf7faff, 0x3c547b, 0xdacefc, 0x9382c0, 0xffd54f);

    private static Appearance appearance = Appearance.LIGHT;
    private static final String ROLE = "chess.role";

    private VisualStyle() {}

    static Appearance appearance() { return appearance; }
    static Colors palette() {
        return switch (appearance) {
            case LIGHT -> LIGHT;
            case DARK -> DARK;
            case HIGH_CONTRAST -> HIGH;
        };
    }

    static void install(Appearance next) {
        appearance = next;
        if (next == Appearance.LIGHT) FlatLightLaf.setup(); else FlatDarkLaf.setup();
        Colors c = palette();
        putColor("Panel.background", c.canvas());
        putColor("OptionPane.background", c.canvas());
        putColor("OptionPane.messageForeground", c.ink());
        putColor("Label.foreground", c.ink());
        UIManager.put("Button.arc", 14);
        UIManager.put("Button.margin", new Insets(6, 11, 6, 11));
        putColor("Button.background", c.surface());
        putColor("Button.foreground", c.ink());
        putColor("Button.borderColor", c.line());
        putColor("Button.hoverBackground", c.inset());
        putColor("Button.disabledText", c.muted());
        putColor("Button.disabledBackground", c.inset());
        Color focus = next == Appearance.HIGH_CONTRAST ? new Color(0xFFDA79) : new Color(0xA77A3F);
        putColor("Component.focusColor", focus);
        putColor("Component.focusedBorderColor", focus);
        putColor("Button.focusedBorderColor", focus);
        putColor("Button.focusedBackground", c.inset());
        UIManager.put("Component.arc", 12);
        putColor("ComboBox.background", c.surface());
        putColor("ComboBox.foreground", c.ink());
        UIManager.put("ScrollPane.border", BorderFactory.createLineBorder(c.line()));
    }

    private static void putColor(String key, Color color) {
        UIManager.put(key, new ColorUIResource(color));
    }

    static <T extends JComponent> T role(T component, Role role) {
        component.putClientProperty(ROLE, role);
        apply(component, role);
        return component;
    }

    static void updateTree(Component root) {
        if (root instanceof JComponent component && component.getClientProperty(ROLE) instanceof Role role) {
            apply(component, role);
        }
        if (root instanceof Container container) {
            for (Component child : container.getComponents()) updateTree(child);
        }
    }

    static JLabel label(String text, float size, boolean bold, Role role) {
        JLabel label = new JLabel(text);
        float scale = Math.max(1f, label.getFont().getSize2D() / 13f);
        label.setFont(label.getFont().deriveFont(bold ? Font.BOLD : Font.PLAIN, size * scale));
        return role(label, role);
    }

    static JPanel card(LayoutManager layout, Role role, int inset) {
        JPanel panel = new RoundedPanel(layout);
        panel.setBorder(new EmptyBorder(inset, inset, inset, inset));
        return role(panel, role);
    }

    private static void apply(JComponent component, Role role) {
        Colors c = palette();
        switch (role) {
            case CANVAS -> component.setBackground(c.canvas());
            case SURFACE, CARD -> {
                component.setBackground(c.surface());
                component.setForeground(c.ink());
            }
            case INK -> component.setForeground(c.ink());
            case MUTED -> component.setForeground(c.muted());
            case STATUS -> component.setBackground(c.forest());
            case STATUS_TEXT -> component.setForeground(c.inverse());
            case STATUS_MUTED -> component.setForeground(c.inverseMuted());
            case BOARD_FRAME -> component.setBackground(c.boardFrame());
            case PRIMARY, DANGER -> {
                // Only override paired button colors with the FlatLaf delegate that paints both.
                if (UIManager.getLookAndFeel() instanceof FlatLaf) {
                    String style = role == Role.PRIMARY
                            ? "background: " + hex(c.action()) + "; foreground: #ffffff; hoverBackground: "
                            + hex(c.hover()) + "; focusedBackground: " + hex(c.action())
                            + "; borderColor: " + hex(c.action())
                            : "foreground: " + hex(c.danger());
                    component.putClientProperty("FlatLaf.style", style);
                }
            }
        }
    }

    private static String hex(Color color) { return String.format(java.util.Locale.ROOT, "#%06x", color.getRGB() & 0xffffff); }

    private static Colors colors(int... colors) {
        Color[] c = java.util.Arrays.stream(colors).mapToObj(Color::new).toArray(Color[]::new);
        return new Colors(c[0],c[1],c[2],c[3],c[4],c[5],c[6],c[7],c[8],
                c[9],c[10],c[11],c[12],c[13],c[14],c[15],c[16],c[17]);
    }

    private static final class RoundedPanel extends JPanel {
        private static final long serialVersionUID = 1L;

        RoundedPanel(LayoutManager layout) {
            super(layout);
            setOpaque(false);
        }

        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(getBackground());
            g.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
            if (getClientProperty(ROLE) == Role.CARD) {
                g.setColor(palette().line());
                g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
            }
            g.dispose();
            super.paintComponent(graphics);
        }
    }
}
