# Assets and dependencies

- The Chess Swing app icon, piece silhouettes, and Home board illustration are original project artwork. The vector pieces and motif are drawn in Java2D by `PieceArt` and `BoardMotif`. No chess-piece font or third-party chess set is used.
- The PNGs and GIF in this package are app-only recordings from the Linux GUI verification. Owner-supplied diagnostic desktop images are excluded.
- FlatLaf 3.6.2 is from FormDev and is distributed under Apache License 2.0. The original binary and [license](../lib/FLATLAF-LICENSE.txt) are included. The build verifies the SHA-256 of the binary before packaging, preserves the license in the runnable JAR, and enables Java's multi-release metadata for the bundled library. [FlatLaf upstream](https://github.com/JFormDesigner/FlatLaf) · [Maven artifact](https://repo.maven.apache.org/maven2/com/formdev/flatlaf/3.6.2/)
