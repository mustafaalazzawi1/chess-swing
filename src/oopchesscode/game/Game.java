package oopchesscode.game;

import oopchesscode.game.ChessMatch.Move;
import oopchesscode.game.ChessMatch.Promotion;
import oopchesscode.utils.Utils;

import javax.swing.SwingUtilities;
import java.awt.GraphicsEnvironment;
import java.util.Locale;
import java.util.Scanner;

/** Launches the original Swing and console game modes. */
public final class Game {
    private Game() {}

    public static void main(String[] args) {
        if (args.length == 0) {
            if (GraphicsEnvironment.isHeadless()) {
                System.err.println("Chess Swing needs a graphical desktop. Run with --console for terminal play.");
                return;
            }
            SwingUtilities.invokeLater(() -> {
                VisualStyle.install(VisualStyle.Appearance.LIGHT);
                new ChessWindow().show();
            });
        } else if (args.length == 1 && "--console".equals(args[0])) {
            playConsole();
        } else {
            System.err.println("Usage: java -jar chess-swing.jar [--console]");
        }
    }

    private static void playConsole() {
        ChessMatch match = new ChessMatch();
        System.out.println("Enter a move such as e2 e4, or e7 e8 n to promote to a knight.");
        System.out.println("Promotion: q/r/b/n. Other commands: draw, f (resign), quit.");
        try (Scanner input = new Scanner(System.in)) {
            while (match.result() == ChessMatch.Result.ACTIVE) {
                match.display();
                System.out.print(match.status() + " > ");
                if (!input.hasNextLine()) return;
                String line = input.nextLine().trim().toLowerCase(Locale.ROOT);
                if (line.equals("quit")) return;
                if (line.equals("f")) {
                    match.resign();
                } else if (line.equals("draw")) {
                    if (!match.claimDraw()) System.out.println("A draw is unavailable in this position.");
                } else {
                    Move move = parseMove(line);
                    if (move == null || !match.play(move)) System.out.println("Illegal move. Include a promotion choice when required.");
                }
            }
            match.display();
            System.out.println(match.status());
        }
    }

    private static Move parseMove(String line) {
        String[] parts = line.split("\\s+");
        if (parts.length < 2 || parts.length > 3
                || !Utils.isValidInput(parts[0]) || !Utils.isValidInput(parts[1])) return null;
        Promotion promotion = null;
        if (parts.length == 3) {
            promotion = switch (parts[2]) {
                case "q" -> Promotion.QUEEN;
                case "r" -> Promotion.ROOK;
                case "b" -> Promotion.BISHOP;
                case "n" -> Promotion.KNIGHT;
                default -> null;
            };
            if (promotion == null) return null;
        }
        return new Move(Utils.ultConversion(parts[0]), Utils.ultConversion(parts[1]), promotion);
    }
}
