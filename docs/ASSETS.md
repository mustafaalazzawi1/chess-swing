# Artwork and dependencies

## Original artwork and media

The app icon, chess-piece silhouettes, and Home board illustration were created for Chess Swing. `PieceArt` and `BoardMotif` draw the vector artwork in Java2D. No third-party chess-piece font or chess set is used.

The screenshots and gameplay GIF show the running Linux application.

## FlatLaf

FlatLaf 3.6.2 is from FormDev and is distributed under Apache License 2.0. The original binary and [license](../lib/FLATLAF-LICENSE.txt) are included.

The build verifies the binary's SHA-256 before packaging, preserves its license in the runnable JAR, and enables Java's multi-release metadata for the bundled library.

[Upstream project](https://github.com/JFormDesigner/FlatLaf) · [Maven artifact](https://repo.maven.apache.org/maven2/com/formdev/flatlaf/3.6.2/)
