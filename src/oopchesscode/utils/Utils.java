
package oopchesscode.utils;

import oopchesscode.position.Position;

public class Utils {
    /**
     * Checks if input string is a valid chessboard coordinate (like "A1", "H8")
     */
    public static boolean isValidInput(String input) {
        if (input == null || input.length() != 2) {
            return false;
        }
        char col = Character.toUpperCase(input.charAt(0));
        char row = input.charAt(1);
        return col >= 'A' && col <= 'H' && row >= '1' && row <= '8';
    }

    /**
     * Converts input string like "A1" to Position object
     */
    public static Position ultConversion(String input) {
        char col = Character.toUpperCase(input.charAt(0));
        char row = input.charAt(1);
        int x = '8' - row;
        int y = col - 'A';
        return new Position(x, y);
    }
}
