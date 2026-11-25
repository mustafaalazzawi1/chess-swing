# Build and verification

## Requirements

- Java 21 or newer to run the JAR.
- A JDK 21 or newer to build from source.
- A POSIX shell for the supplied scripts: Terminal, Bash, or Git Bash.
- A graphical display for the Swing interface and layout checks.

The repository includes FlatLaf 3.6.2 and its license. The build checks the bundled binary's SHA-256 and does not download dependencies.

## Run the release

Download `chess-swing-v1.2.0.jar` from [Releases](https://github.com/mustafaalazzawi1/chess-swing/releases/tag/v1.2.0), then run it from its download folder:

```sh
java -jar chess-swing-v1.2.0.jar
```

The release also provides the complete source ZIP and `SHA256SUMS.txt`. To check both downloaded assets, place all three files in one folder and run:

```sh
sha256sum -c SHA256SUMS.txt
```

On macOS, use `shasum -a 256 -c SHA256SUMS.txt`.

## Build from source

Extract the release source ZIP or clone the repository. From the project root:

```sh
./build.sh --test
./scripts/smoke-jar.sh
java -jar build/chess-swing.jar
```

The first command compiles the application, builds the runnable JAR, and runs the 40 automated test groups. The smoke test checks the packaged console entry point and icon resource.

For headful layout checks after building the tests:

```sh
java -cp build/classes:build/test-classes oopchesscode.game.ChessWindowLayoutTest
```

The classpath command above is for Linux and macOS. The layout check requires a graphical display.

## Build a macOS app

Use a JDK matching the target CPU: arm64 for Apple silicon or x64 for Intel. Extract the source ZIP into Downloads, then run:

```sh
cd ~/Downloads/chess-swing-v1.2.0-source
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
bash scripts/package-macos.sh
```

For another supported JDK, set `JAVA_HOME` to its JDK home.

The script runs the build and tests, creates the ICNS icon, and uses `jpackage` to bundle a Java runtime. Extract `build/macos/ChessSwing-macos-arm64.zip` or `build/macos/ChessSwing-macos-x86_64.zip`, then open **Chess Swing.app** from Finder.

Check the version and Dock icon, computer play, clocks, PGN import/export, and recovery. To verify runtime isolation, repeat on a Mac of the same architecture without a system JDK.

A prebuilt v1.2.0 native Mac app is not distributed. Its Finder launch, signing, and notarization have not been verified.

## Verification coverage

| Area | Recorded result |
| --- | --- |
| Rules, notation, files, replay, search, clocks, and puzzles | 40 automated groups passed on Linux, including a clean build from the release source ZIP. |
| Packaged JAR | Console checkmate and icon-resource smoke test passed. |
| Hosted release source | [GitHub Actions passed](https://github.com/mustafaalazzawi1/chess-swing/actions/runs/45368356365) against commit `b1de0bcfb40f5b52d1ed730b6430cd4969424dd5`. |
| Release downloads | The [publishing job](https://github.com/mustafaalazzawi1/chess-swing/actions/runs/45370348643) downloaded all three assets anonymously and matched their bytes to the verified build. |
| Linux GUI | Computer play, hints, practice, replay, PGN, and promotion were exercised. Layout checks covered all three themes, default and minimum sizes, enlarged fonts, and visual inspection at 125% scale. |
| macOS v1.2.0 JAR | Limited launch and interface checks covered Home, Resume, and Practice. Full v1.2.0 Mac regression testing is not recorded. |
| Earlier macOS checks | v1.0.1 covered computer and timed play. The earlier Apple-silicon native bundle built and launched. These checks do not establish v1.2.0 native-bundle verification. |

## Continuous integration and release source

The build workflow runs on pushes and pull requests and can also verify a specified source revision manually. Its Actions are pinned, and it checks the expected Temurin build.

The manual v1.2.0 packaging and publishing workflows use the fixed release commit and verify the JAR and source ZIP hashes. The publishing job alone has repository write permission.

The v1.2.0 tag and downloadable source ZIP are fixed release snapshots. Later documentation and workflow updates on `main` do not replace those release files.

### Current source maintenance

The post-v1.2.0 source separates board, Home, and practice views from the window controller. `./build.sh --test` now adds three view-contract checks after the existing 40 engine/session groups. CI additionally executes `ChessWindowLayoutTest` under Xvfb. These source checks do not change or retest the already published v1.2.0 binary.
