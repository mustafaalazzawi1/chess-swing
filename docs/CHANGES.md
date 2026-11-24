# Changelog

## Repository updates after v1.2.0

- Reorganized the README around the application, downloads, features, and verification.
- Added a user guide and separate build, Mac packaging, and platform-verification documentation.
- Updated architecture notes and verification status.
- Corrected hosted Java setup and added workflows to verify, package, and publish the fixed v1.2.0 source.
- Application behavior, version, release tag, and downloadable asset hashes are unchanged.

## v1.2.0 — Visual redesign

- Flexible Home screen with original board artwork, mode cards, and Resume for an existing game.
- Light, dark, and high-contrast appearances across play, practice, setup, and dialogs.
- Original Java2D pieces and distinct markers for legal moves, last moves, hints, check, and keyboard focus.
- Consistent history, clock, and capture displays with grouped controls.
- Pinned and checksum-verified FlatLaf 3.6.2 dependency for offline builds.
- Existing game rules and computer reply timing preserved.

## v1.1.0 — Learning and usability

- Home entry points for computer play, local play, practice, PGN import, and recovery.
- Legal engine hints without automatically playing a move or pausing the live clock.
- Five mate-in-one and three mate-in-two puzzles accepting alternative winning moves.
- Independent practice state and paused live-game recovery from Home.
