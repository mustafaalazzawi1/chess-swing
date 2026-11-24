# Changes

## v1.2.0 visual redesign

- Flexible Home with original board artwork, actionable mode cards, and Resume when a game exists.
- Light, dark, and high-contrast appearances shared by play, practice, setup, and dialogs.
- Original Java2D chess pieces, distinct board markers, aligned history, clock and capture cards, and grouped actions with keyboard focus.
- FlatLaf 3.6.2 is pinned, checksum-verified, and bundled for reproducible offline builds. The existing game logic and timing are unchanged.

## v1.1.0 learning and usability

- The welcome screen offers computer play, local play, practice, PGN import, and resume. A recovered game is ready from Home with the clock paused.
- An on-demand hint offers one legal engine suggestion without committing a move or pausing the clock. Computer reply timing remains unchanged.
- Eight original checkmate exercises use the existing rules engine to validate all winning alternatives and defender replies. Practice state stays in memory and separate from live games.
- The JAR and macOS packaging script use v1.1.0. This pass was tested on Linux. See the README for the owner's prior Mac evidence and the new Mac retest.
