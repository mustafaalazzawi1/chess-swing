# User guide

Chess Swing is a Java desktop application for local play and practice. For installation and packaging, see [Build and verification](BUILD-AND-VERIFICATION.md).

## Choose how to play

- **Play computer** opens setup with the computer selected. Choose your side, difficulty, and time per side. **Local two-player** opens setup for two people sharing the board.
- **Open saved PGN…** imports a game. **Resume game** opens the current or recovered game. Home and practice pause live clocks and cancel computer work until you resume. Starting or importing a replacement still prompts before discarding unsaved progress.
- **Practice puzzles** contains five mate-in-one and three mate-in-two exercises. All legal winning moves are accepted. A move that misses the objective leaves the board in place. In mate-in-two puzzles, the app plays a legal defense before asking for the final mate. Use **Reset puzzle**, **Next puzzle**, or **Show solution…**. Practice progress is kept only while the app is open.
- **Hint** or **Ctrl+J** offers a legal move on the human side's turn in an active game. In local play, either player may request a hint on their own turn. Two outlined squares show the suggestion. It does not play a move, edit recovery, or pause either clock. Hints are unavailable in replay, practice, dialogs, computer turns, and ended games.

## Game controls

| Action | Control |
| --- | --- |
| Start a game | **Play computer**, **Local two-player**, or **New game…** (Ctrl+N) |
| Request a hint | **Hint** or Ctrl+J on your turn |
| Return to a game | **Resume game** from Home or **Back to game** from practice |
| Move a piece | Click a piece and destination, or focus the board with Ctrl+B and use arrows plus Enter or Space |
| Clear selection | Esc or click the selected square |
| Replay a position | Select a move in history or use **Start**, **Back**, and **Next** |
| Replay shortcuts | Alt+Home, Alt+Left, Alt+Right, and Alt+End to return live |
| Flip or boost contrast | F to flip; Ctrl+H to toggle the high-contrast board |
| Open or save PGN | Ctrl+O or Ctrl+S |
| End a game | Use **Claim draw** when enabled, or **Resign** |

### Difficulty

| Level | Maximum depth | Search budget |
| --- | ---: | ---: |
| Casual | 1 half-move | 125 ms |
| Standard | 2 half-moves | 400 ms |
| Deep | 3 half-moves | 850 ms |

The engine searches progressively deeper with alpha-beta pruning. It keeps the last fully searched choice, with a legal fallback if the budget expires early. The budgets are checked between search operations and are approximate wall-clock targets. There are no Elo estimates.

### Clocks, takebacks, and recovery

- Timed games use a per-side countdown with no increment. The live side's clock runs during a computer search as well as a human turn. At zero the game ends before a pending move may be accepted. The clock stops when the game ends.
- Replay pauses the live clock and shows the recorded times at the displayed move. Setup, file and confirmation dialogs, promotion selection, and import validation pause live time and cancel computer work. Leaving replay or closing a dialog restores the live turn. Outside the app's dialogs, live clocks continue even if the window loses focus.
- Local takebacks require both players' confirmation and rewind one half-move. In computer mode, one confirmation returns to the human's previous choice and removes the reply if it is already recorded. The board, rule state, capture display, and both clocks rewind together.
- The app writes a local recovery PGN at `~/.chess-swing/recovery.pgn` after moves and transitions and roughly every five seconds in timed play. Recovery restores opponent, difficulty, side, checkpoints, and the most recently saved clock reading. Time away from the app does not count. A sudden termination can lose time elapsed since the last write.
- Regular PGN export includes SAN and the nominal time control when applicable. PGN import opens the main line as an untimed local game. Per-move clock snapshots and computer settings belong to local recovery; saving during replay exports the full live move list.


## Appearance and board markers

Choose **View → Light**, **Dark**, or **High contrast**. **Ctrl+H** switches between high contrast and the previous appearance. Theme changes preserve the live game.

- Corner wedges mark the last move.
- A solid frame marks selection; a double frame marks a king in check.
- A dot or circle marks a legal move or capture.
- Rounded outlines mark a hint; a dotted ring marks keyboard focus.
- A triangle beside the player's name identifies the live side to move.

[Light gameplay](screenshots/gameplay.png) · [Dark gameplay](screenshots/dark-gameplay.png) · [Compact high contrast](screenshots/high-contrast.png)

## Console mode

Run `java -jar chess-swing-v1.2.0.jar --console` for an untimed local game in the terminal. Enter coordinates such as `e2 e4`, or `e7 e8 n` for promotion. Enter `draw` to claim an available draw, `f` to resign, or `quit` to exit.

## File and practice limits

PGN import accepts one UTF-8 standard-chess game per file, up to 512 KiB, and retains its main line. Comments, annotations, and variations are skipped. Imported games open as untimed local games.

Practice progress is kept in memory while the app is open. The live game uses a separate recovery copy; practice moves never change it.
