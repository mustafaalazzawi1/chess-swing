package oopchesscode.game;

import oopchesscode.game.ChessMatch.Move;
import oopchesscode.game.ChessMatch.Promotion;
import oopchesscode.pieces.Piece;
import oopchesscode.position.Position;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import static oopchesscode.game.VisualStyle.Role.*;
import static oopchesscode.game.VisualStyle.label;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;
import oopchesscode.game.MatchSession.Setup;
import oopchesscode.game.MatchSession.Opponent;

final class ChessWindow {
    private static final Color DARK_MARK = new Color(0x142923);
    private static final Color CHECK = new Color(0xEEA18B);
    private static final int BASE_SIDEBAR_WIDTH = 330;
    private static final int CONTROL_GAP = 6;
    private static final int BOARD_INSET = 8;

    private enum Screen { HOME, GAME, PRACTICE }

    private final CardLayout screenCards = new CardLayout();
    private final JPanel screens = new JPanel(screenCards);
    private final CardLayout sidebarCards = new CardLayout();
    private final JPanel sidebars = new JPanel(sidebarCards);
    private final JButton resume = button("Resume game", "Continue the current or recovered game");
    private final JButton hintButton = button("Hint", "Suggest a legal move (Ctrl+J)");
    private JButton playComputer;
    private JButton backToGame;
    private final JLabel homeStatus = label("", 14, false, MUTED);
    private final JLabel puzzleTitle = label("", 20, true, INK);
    private final JLabel puzzleNumber = label("", 12, true, MUTED);
    private final JLabel puzzleGoal = label("", 15, true, INK);
    private final JTextArea puzzleFeedback = new JTextArea(3, 20);
    private Screen screen = Screen.HOME;
    private boolean hasGame;
    private PuzzleSession practice;
    private boolean practiceFlipped;
    private SwingWorker<Move, Void> hintWorker;
    private Move suggestedMove;
    private String hintMessage;

    private final JFrame frame = new JFrame("Chess Swing");
    private final Square[][] squares = new Square[8][8];
    private final JLabel mode = label("Live game", 12, true, STATUS_MUTED);
    private final JLabel status = label("", 19, true, STATUS_TEXT);
    private final float statusSize = status.getFont().getSize2D();
    private final JLabel hint = label("", 12, false, STATUS_MUTED);
    private final JLabel storage = label("", 12, false, MUTED);
    private final DefaultListModel<String> history = new DefaultListModel<>();
    private final JList<String> moveList = new JList<>(history);
    private final JButton first = button("Start", "Start of game (Alt+Home)");
    private final JButton previous = button("Back", "Previous position (Alt+Left)");
    private final JButton next = button("Next", "Next position (Alt+Right)");
    private final JButton live = button("Live", "Return to live game (Alt+End)");
    private final JButton takeback = button("Takeback…", "Request both players' consent");
    private final JButton draw = button("Claim draw", "Claim an available draw");
    private final JButton resign = button("Resign…", "Resign the current side");
    private final JRadioButtonMenuItem contrast = new JRadioButtonMenuItem("High contrast");
    private final JRadioButtonMenuItem light = new JRadioButtonMenuItem("Light", true);
    private final JRadioButtonMenuItem dark = new JRadioButtonMenuItem("Dark");
    private final JLabel whitePlayer = label("White", 14, true, INK);
    private final JLabel blackPlayer = label("Black", 14, true, INK);
    private JPanel gameSidebar;
    private JPanel gameActions;
    private VisualStyle.Appearance regularAppearance = VisualStyle.Appearance.LIGHT;
    private final DesktopFiles files = new DesktopFiles(frame, storage);
    private MatchSession session = MatchSession.create(Setup.DEFAULT, System::nanoTime);
    private final JLabel whiteClock = label("", 16, true, INK);
    private final JLabel blackClock = label("", 16, true, INK);
    private final JLabel whiteCaptures = label("", 13, false, MUTED);
    private final JLabel blackCaptures = label("", 13, false, MUTED);
    private final Timer timer = new Timer(100, event -> tick());
    private SwingWorker<Move, Void> search;
    private int modalDepth;
    private boolean closing;
    private long lastCheckpoint;
    private ChessMatch replay;
    private int replayPly;
    private Position selected;
    private boolean flipped;
    private boolean updatingHistory;
    private List<String> renderedMoves = List.of();
    private StartPosition renderedStart = StartPosition.STANDARD;
    private Square boardFocus;
    private boolean busy;

    void show() {
        JPanel root = VisualStyle.role(new JPanel(new BorderLayout(24, 16)), CANVAS);
        root.setBorder(new EmptyBorder(14, 22, 18, 22));
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JPanel title = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        title.setOpaque(false);
        JLabel brand = label("Chess Swing", 25, true, INK);
        var brandResource = ChessWindow.class.getResource("/assets/chess-swing.png");
        if (brandResource != null) {
            brand.setIcon(new ImageIcon(new ImageIcon(brandResource).getImage().getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
            brand.setIconTextGap(9);
        }
        title.add(brand);
        String version = Game.class.getPackage().getImplementationVersion();
        if (version != null) title.add(label("v" + version, 12, true, MUTED));
        header.add(title, BorderLayout.WEST);
        JButton setup = button("New game…", "New game setup (Ctrl+N)");
        VisualStyle.role(setup, PRIMARY);
        setup.addActionListener(event -> newGame());
        JButton home = button("Home", "Open the start screen");
        home.addActionListener(event -> showScreen(Screen.HOME));
        JPanel navigation = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        navigation.setOpaque(false);
        navigation.add(home);
        navigation.add(setup);
        header.add(navigation, BorderLayout.EAST);
        root.add(header, BorderLayout.NORTH);
        JPanel arena = new JPanel(new BorderLayout(20, 0));
        arena.setOpaque(false);
        arena.add(boardHolder(), BorderLayout.CENTER);
        JPanel sidebar = sidebar();
        gameSidebar = sidebar;
        sidebars.setOpaque(false);
        sidebars.add(sidebar, Screen.GAME.name());
        sidebars.add(practiceSidebar(), Screen.PRACTICE.name());
        arena.add(sidebars, BorderLayout.EAST);
        screens.setOpaque(false);
        screens.add(welcome(), Screen.HOME.name());
        screens.add(arena, Screen.GAME.name());
        root.add(screens, BorderLayout.CENTER);
        bind(root, KeyEvent.VK_ESCAPE, 0, "clear", () -> { clearHint(); resetSelection(); });
        bind(root, KeyEvent.VK_F, 0, "flip", this::flip);
        bind(root, KeyEvent.VK_B, KeyEvent.CTRL_DOWN_MASK, "focus-board", this::focusBoard);
        bind(root, KeyEvent.VK_J, KeyEvent.CTRL_DOWN_MASK, "hint", this::requestHint);
        bind(root, KeyEvent.VK_HOME, KeyEvent.ALT_DOWN_MASK, "first", () -> goTo(0));
        bind(root, KeyEvent.VK_LEFT, KeyEvent.ALT_DOWN_MASK, "previous", () -> goTo(currentPly() - 1));
        bind(root, KeyEvent.VK_RIGHT, KeyEvent.ALT_DOWN_MASK, "next", () -> goTo(currentPly() + 1));
        bind(root, KeyEvent.VK_END, KeyEvent.ALT_DOWN_MASK, "live", this::returnLive);

        frame.setContentPane(root);
        frame.setJMenuBar(menus());
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent event) { close(); }
        });
        var icon = ChessWindow.class.getResource("/assets/chess-swing.png");
        if (icon != null) frame.setIconImage(new ImageIcon(icon).getImage());
        updateMinimumSize();
        frame.setSize(Math.max(1120, frame.getMinimumSize().width), Math.max(780, frame.getMinimumSize().height));
        frame.setLocationRelativeTo(null);
        refresh();
        frame.setVisible(true);
        MatchSession recovered = files.recover();
        if (recovered != null) {
            session = recovered;
            hasGame = true;
            flipped = session.versusComputer() && !session.setup.humanWhite();
        }
        lastCheckpoint = System.nanoTime();
        showScreen(Screen.HOME);
        timer.start();
    }

    private JPanel welcome() {
        JPanel holder = new JPanel(new GridBagLayout());
        holder.setOpaque(false);
        JPanel hero = VisualStyle.card(new BorderLayout(0, 12), STATUS, 24);
        JPanel headline = new JPanel(new GridLayout(2, 1, 0, 4));
        headline.setOpaque(false);
        headline.add(label("Chess at", 34, true, STATUS_TEXT));
        headline.add(label("your pace", 34, true, STATUS_TEXT));
        hero.add(headline, BorderLayout.NORTH);
        hero.add(new BoardMotif(), BorderLayout.CENTER);

        JPanel menu = new JPanel(new BorderLayout(0, 14));
        menu.setOpaque(false);
        JPanel heading = new JPanel(new GridLayout(2, 1, 0, 3));
        heading.setOpaque(false);
        heading.add(label("Choose how to play", 24, true, INK));
        heading.add(homeStatus);
        menu.add(heading, BorderLayout.NORTH);
        JPanel choices = new JPanel(new GridLayout(2, 2, 12, 12));
        choices.setOpaque(false);
        playComputer = welcomeAction("Play computer", "Choose a side, level and clock.", () -> newGame(Opponent.COMPUTER));
        choices.add(modeCard(playComputer, "Choose a side, level and clock.", 'N'));
        choices.add(modeCard(welcomeAction("Local two-player", "Share the board on one device.", () -> newGame(Opponent.LOCAL)),
                "Share the board on one device.", 'P'));
        choices.add(modeCard(welcomeAction("Practice puzzles", "Solve eight checkmate positions.", this::openPractice),
                "Solve eight checkmate positions.", 'Q'));
        choices.add(modeCard(welcomeAction("Open saved PGN…", "Continue a game or review moves.", this::openPgn),
                "Continue a game or review moves.", 'R'));
        menu.add(choices, BorderLayout.CENTER);
        VisualStyle.role(resume, PRIMARY);
        resume.addActionListener(event -> resumeGame());
        menu.add(resume, BorderLayout.SOUTH);

        GridBagConstraints left = new GridBagConstraints();
        left.gridx = 0;
        left.weightx = .42;
        left.weighty = 1;
        left.fill = GridBagConstraints.BOTH;
        left.insets = new Insets(12, 0, 6, 22);
        holder.add(hero, left);
        GridBagConstraints right = new GridBagConstraints();
        right.gridx = 1;
        right.weightx = .58;
        right.weighty = 1;
        right.fill = GridBagConstraints.BOTH;
        right.insets = new Insets(12, 0, 6, 0);
        holder.add(menu, right);
        return holder;
    }

    private JPanel modeCard(JButton action, String description, char piece) {
        JPanel card = VisualStyle.card(new GridBagLayout(), CARD, 13);
        JPanel content = new JPanel(new BorderLayout(0, 8));
        content.setOpaque(false);
        JLabel art = label("", 13, false, MUTED);
        art.setIcon(new Icon() {
            @Override public int getIconWidth() { return 42; }
            @Override public int getIconHeight() { return 42; }
            @Override public void paintIcon(Component component, Graphics g, int x, int y) {
                PieceArt.paint((Graphics2D) g, x, y, 42, piece, true);
            }
        });
        content.add(art, BorderLayout.NORTH);
        JLabel summary = label("<html><body style='width:118px'>" + description + "</body></html>", 13, false, MUTED);
        content.add(summary, BorderLayout.CENTER);
        content.add(action, BorderLayout.SOUTH);
        GridBagConstraints fill = new GridBagConstraints();
        fill.weightx = 1;
        fill.fill = GridBagConstraints.HORIZONTAL;
        card.add(content, fill);

        var mouse = new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent event) {
                if (SwingUtilities.isLeftMouseButton(event)) action.doClick();
            }
            @Override public void mouseEntered(java.awt.event.MouseEvent event) { action.getModel().setRollover(true); }
            @Override public void mouseExited(java.awt.event.MouseEvent event) { action.getModel().setRollover(false); }
        };
        for (Component target : List.of(card, content, art, summary)) target.addMouseListener(mouse);
        return card;
    }

    private JButton welcomeAction(String text, String description, Runnable action) {
        JButton button = button(text, description);
        button.addActionListener(event -> action.run());
        return button;
    }

    private JPanel practiceSidebar() {
        JPanel panel = VisualStyle.card(new BorderLayout(0, 16), CARD, 14);
        JPanel heading = VisualStyle.card(new GridLayout(3, 1, 0, 7), STATUS, 14);
        VisualStyle.role(puzzleNumber, STATUS_MUTED);
        VisualStyle.role(puzzleTitle, STATUS_TEXT);
        VisualStyle.role(puzzleGoal, STATUS_TEXT);
        heading.add(puzzleNumber);
        heading.add(puzzleTitle);
        heading.add(puzzleGoal);
        panel.add(heading, BorderLayout.NORTH);
        puzzleFeedback.setEditable(false);
        puzzleFeedback.setFocusable(false);
        puzzleFeedback.setLineWrap(true);
        puzzleFeedback.setWrapStyleWord(true);
        puzzleFeedback.setFont(puzzleFeedback.getFont().deriveFont(Math.max(14f, UIManager.getFont("Label.font").getSize2D())));
        VisualStyle.role(puzzleFeedback, SURFACE);
        puzzleFeedback.setBorder(new EmptyBorder(3, 6, 3, 6));
        puzzleFeedback.getAccessibleContext().setAccessibleName("Puzzle feedback");
        panel.add(puzzleFeedback, BorderLayout.CENTER);

        JButton reset = button("Reset puzzle", "Restore the original puzzle position");
        reset.addActionListener(event -> resetPractice(false));
        JButton nextPuzzle = button("Next puzzle", "Go to the next exercise");
        nextPuzzle.addActionListener(event -> resetPractice(true));
        JButton solution = button("Show solution…", "Explain the idea and list accepted moves");
        solution.addActionListener(event -> {
            if (!busy) JOptionPane.showMessageDialog(frame, practice.explanation(),
                    "Puzzle solution", JOptionPane.INFORMATION_MESSAGE);
        });
        backToGame = button("Back to game", "Resume the live or recovered game, or return home");
        backToGame.addActionListener(event -> resumeGame());
        JPanel controls = new JPanel(new GridLayout(2, 1, 0, 6));
        controls.setOpaque(false);
        VisualStyle.role(nextPuzzle, PRIMARY);
        controls.add(row(reset, nextPuzzle));
        controls.add(row(solution, backToGame));
        panel.add(controls, BorderLayout.SOUTH);
        return panel;
    }

    private void showScreen(Screen nextScreen) {
        if (busy || closing) return;
        if (screen != nextScreen) {
            clearHint();
            selected = null;
        }
        screen = nextScreen;
        resume.setEnabled(hasGame);
        resume.setVisible(hasGame);
        homeStatus.setText(hasGame ? "Your game is paused and ready to resume."
                : "Start a game, open a PGN, or try a puzzle.");
        screenCards.show(screens, screen == Screen.HOME ? Screen.HOME.name() : Screen.GAME.name());
        if (screen != Screen.HOME) sidebarCards.show(sidebars, screen.name());
        syncActivity();
        if (screen != Screen.HOME) focusBoard();
        else (hasGame ? resume : playComputer).requestFocusInWindow();
    }

    private void resumeGame() {
        if (hasGame) showScreen(Screen.GAME);
        else showScreen(Screen.HOME);
    }

    private void openPractice() {
        if (busy) return;
        pauseActivity();
        if (practice == null) {
            practice = new PuzzleSession();
            practiceFlipped = !practice.board().whiteTurn();
        }
        showScreen(Screen.PRACTICE);
    }

    private void resetPractice(boolean nextPuzzle) {
        if (busy) return;
        if (nextPuzzle) practice.next();
        else practice.reset();
        practiceFlipped = !practice.board().whiteTurn();
        resetSelection();
    }

    private JPanel boardHolder() {
        JPanel board = VisualStyle.card(new GridLayout(8, 8), BOARD_FRAME, BOARD_INSET);
        board.getAccessibleContext().setAccessibleName("Chessboard");
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                squares[row][col] = new Square(row, col);
                board.add(squares[row][col]);
            }
        }
        boardFocus = squares[6][4];
        boardFocus.setFocusable(true);
        JPanel holder = new JPanel(null) {
            @Override public void doLayout() {
                int space = Math.min(getWidth(), getHeight());
                int padding = BOARD_INSET * 2;
                int size = space < padding ? 0 : padding + (space - padding) / 8 * 8;
                board.setBounds((getWidth() - size) / 2, (getHeight() - size) / 2, size, size);
            }
        };
        holder.setOpaque(false);
        holder.add(board);
        return holder;
    }

    private JMenuBar menus() {
        JMenuBar bar = new JMenuBar();
        JMenu file = new JMenu("File");
        file.setMnemonic(KeyEvent.VK_F);
        file.add(item("New game…", KeyEvent.VK_N, this::newGame));
        file.add(item("Open saved PGN…", KeyEvent.VK_O, this::openPgn));
        file.add(item("Home", 0, () -> showScreen(Screen.HOME)));
        file.add(item("Save PGN…", KeyEvent.VK_S, () -> {
            if (!busy) withPause(() -> files.save(session.exportGame()));
        }));
        file.addSeparator();
        file.add(item("Exit", KeyEvent.VK_Q, this::close));
        JMenu view = new JMenu("View");
        view.setMnemonic(KeyEvent.VK_V);
        view.add(item("Flip board", 0, this::flip));
        ButtonGroup appearances = new ButtonGroup();
        appearances.add(contrast);
        appearances.add(light);
        appearances.add(dark);
        frame.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_H, KeyEvent.CTRL_DOWN_MASK), "contrast");
        frame.getRootPane().getActionMap().put("contrast", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent event) {
                if (VisualStyle.appearance() == VisualStyle.Appearance.HIGH_CONTRAST) applyAppearance(regularAppearance);
                else applyAppearance(VisualStyle.Appearance.HIGH_CONTRAST);
            }
        });
        contrast.addActionListener(event -> applyAppearance(VisualStyle.Appearance.HIGH_CONTRAST));
        light.addActionListener(event -> applyAppearance(VisualStyle.Appearance.LIGHT));
        dark.addActionListener(event -> applyAppearance(VisualStyle.Appearance.DARK));
        view.add(contrast);
        view.add(light);
        view.add(dark);
        bar.add(file);
        bar.add(view);
        return bar;
    }

    private void applyAppearance(VisualStyle.Appearance appearance) {
        if (VisualStyle.appearance() == appearance) return;
        if (appearance != VisualStyle.Appearance.HIGH_CONTRAST) regularAppearance = appearance;
        light.setSelected(appearance == VisualStyle.Appearance.LIGHT);
        dark.setSelected(appearance == VisualStyle.Appearance.DARK);
        contrast.setSelected(appearance == VisualStyle.Appearance.HIGH_CONTRAST);
        VisualStyle.install(appearance);
        boolean priorHistoryUpdate = updatingHistory;
        updatingHistory = true;
        try {
            SwingUtilities.updateComponentTreeUI(frame);
            VisualStyle.updateTree(frame);
            layoutSidebar(gameSidebar, gameActions);
            updateMinimumSize();
        } finally {
            updatingHistory = priorHistoryUpdate;
        }
        refresh();
    }

    private void updateMinimumSize() {
        Dimension sidebar = sidebars.getPreferredSize();
        Dimension home = screens.getComponent(0).getPreferredSize();
        frame.setMinimumSize(new Dimension(Math.max(home.width + 54, sidebar.width + 375),
                Math.max(574, home.height + 108)));
        if (frame.isShowing()) {
            Dimension minimum = frame.getMinimumSize();
            frame.setSize(Math.max(frame.getWidth(), minimum.width), Math.max(frame.getHeight(), minimum.height));
            frame.validate();
        }
    }

    private void openPgn() {
        if (busy) return;
        withPause(() -> {
            files.open(incoming -> {
                if (files.allowReplace(session.exportGame(), "open another game", false)) {
                    replace(MatchSession.imported(incoming, System::nanoTime), false);
                }
            }, loading -> {
                busy = loading;
                if (busy) screens.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                else screens.setCursor(Cursor.getDefaultCursor());
                syncActivity();
            });
            return null;
        });
    }

    private static JMenuItem item(String title, int key, Runnable action) {
        JMenuItem item = new JMenuItem(title);
        if (key != 0) item.setAccelerator(KeyStroke.getKeyStroke(key, KeyEvent.CTRL_DOWN_MASK));
        item.addActionListener(event -> action.run());
        return item;
    }

    private JPanel sidebar() {
        JPanel panel = new JPanel(new BorderLayout(0, 9));
        panel.setOpaque(false);
        JPanel top = VisualStyle.card(new GridLayout(3, 1, 0, 4), STATUS, 13);
        // Separate rows keep the larger status and the shorter context readable on resize.
        top.add(mode);
        top.add(status);
        top.add(hint);
        JPanel overview = new JPanel(new BorderLayout(0, 7));
        overview.setOpaque(false);
        overview.add(top, BorderLayout.NORTH);
        JPanel players = new JPanel(new GridLayout(2, 1, 0, 3));
        players.setOpaque(false);
        players.add(playerRow(whitePlayer, whiteClock, whiteCaptures));
        players.add(playerRow(blackPlayer, blackClock, blackCaptures));
        overview.add(players, BorderLayout.SOUTH);
        panel.add(overview, BorderLayout.NORTH);

        JPanel moves = VisualStyle.card(new BorderLayout(0, 7), CARD, 11);
        JLabel historyLabel = label("Move history", 15, true, INK);
        historyLabel.setLabelFor(moveList);
        moves.add(historyLabel, BorderLayout.NORTH);
        moveList.getAccessibleContext().setAccessibleName("Move history. Select a move to replay.");
        moveList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        VisualStyle.role(moveList, SURFACE);
        int historySize = Math.max(14, UIManager.getFont("Label.font").getSize());
        moveList.setFont(new Font(Font.MONOSPACED, Font.PLAIN, historySize));
        moveList.setFixedCellHeight(moveList.getFontMetrics(moveList.getFont()).getHeight() + 8);
        moveList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                JLabel cell = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                cell.setBorder(new EmptyBorder(0, 10, 0, 8));
                var colors = VisualStyle.palette();
                cell.setBackground(isSelected ? colors.action() : index % 2 != 0 ? colors.inset() : colors.surface());
                cell.setForeground(isSelected ? Color.WHITE : colors.ink());
                return cell;
            }
        });
        moveList.addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting() && !updatingHistory && moveList.getSelectedIndex() >= 0) {
                goTo(moveList.getSelectedIndex() + 1);
            }
        });
        JScrollPane scroll = new JScrollPane(moveList);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        moves.add(scroll, BorderLayout.CENTER);
        panel.add(moves, BorderLayout.CENTER);

        JPanel controls = new JPanel(new GridLayout(3, 1, 0, 6));
        gameActions = controls;
        controls.setOpaque(false);
        first.addActionListener(event -> goTo(0));
        previous.addActionListener(event -> goTo(currentPly() - 1));
        next.addActionListener(event -> goTo(currentPly() + 1));
        live.addActionListener(event -> returnLive());
        takeback.addActionListener(event -> requestTakeback());
        draw.addActionListener(event -> withPause(() -> {
            if (session.live().claimDraw()) changed();
            return null;
        }));
        resign.addActionListener(event -> {
            withPause(() -> {
                if (session.live().result() == ChessMatch.Result.ACTIVE
                        && confirm(side(session.live().whiteTurn()) + ", resign this game?", "Resign")) {
                    session.live().resign();
                    changed();
                }
                return null;
            });
        });
        JPanel replayControls = row(first, previous, next, live);
        JPanel gameControls = row(takeback, draw, resign);
        VisualStyle.role(hintButton, PRIMARY);
        VisualStyle.role(resign, DANGER);
        controls.add(replayControls);
        controls.add(gameControls);
        JPanel tools = new JPanel(new BorderLayout(8, 0));
        tools.setOpaque(false);
                tools.add(storage, BorderLayout.CENTER);
        tools.add(hintButton, BorderLayout.EAST);
        hintButton.addActionListener(event -> requestHint());
        controls.add(tools);
        panel.add(controls, BorderLayout.SOUTH);
        // GridLayout gives every button in a row the same width. Size each row
        // using its widest button and the active look and feel's preferred insets.
        panel.setPreferredSize(null);
        panel.setMinimumSize(new Dimension(BASE_SIDEBAR_WIDTH, 0));
        layoutSidebar(panel, controls);
        return panel;
    }

    private static void layoutSidebar(JPanel panel, JPanel controls) {
        panel.setPreferredSize(new Dimension(Math.max(controls.getPreferredSize().width, BASE_SIDEBAR_WIDTH), 400));
    }

    private static JPanel playerRow(JLabel name, JLabel clock, JLabel captures) {
        JPanel row = VisualStyle.card(new BorderLayout(8, 0), CARD, 8);
        row.add(name, BorderLayout.WEST);
        row.add(captures, BorderLayout.CENTER);
        row.add(clock, BorderLayout.EAST);
        return row;
    }

    private static JPanel row(JButton... buttons) {
        JPanel row = new JPanel(new GridLayout(1, buttons.length, CONTROL_GAP, 0));
        row.setOpaque(false);
        for (JButton button : buttons) row.add(button);
        return row;
    }

    private void newGame() { newGame(session.setup.opponent()); }

    private void newGame(Opponent opponent) {
        if (busy) return;
        withPause(() -> {
            Setup setup = GameSetupDialog.choose(frame, new Setup(opponent,
                    session.setup.humanWhite(), session.setup.level(), session.setup.control()));
            if (setup != null && files.allowReplace(session.exportGame(), "start a new game", false)) {
                replace(MatchSession.create(setup, System::nanoTime), false);
            }
            return null;
        });
    }

    private void close() {
        if (busy || closing) return;
        withPause(() -> {
            if (files.allowReplace(session.exportGame(), "exit", true)) {
                closing = true;
                timer.stop();
                files.close();
                frame.dispose();
            }
            return null;
        });
    }

    private void replace(MatchSession replacement, boolean dirty) {
        cancelSearch();
        clearHint();
        session.clock.pause();
        session = replacement;
        hasGame = true;
        screen = Screen.GAME;
        screenCards.show(screens, Screen.GAME.name());
        sidebarCards.show(sidebars, Screen.GAME.name());
        replay = null;
        selected = null;
        flipped = session.versusComputer() && !session.setup.humanWhite();
        files.autosave(session, dirty);
        lastCheckpoint = System.nanoTime();
        syncActivity();
        focusBoard();
    }

    private void changed() {
        cancelSearch();
        clearHint();
        selected = null;
        files.autosave(session, true);
        lastCheckpoint = System.nanoTime();
        syncActivity();
    }

    private void requestTakeback() {
        if (screen != Screen.GAME || busy || replay != null || session.takebackTarget() < 0) return;
        withPause(() -> {
            boolean moverIsWhite = !session.live().whiteTurn();
            String last = session.live().notation().getLast();
            boolean accepted = session.versusComputer()
                    ? confirm("Return to your previous decision and restore both clocks?", "Takeback")
                    : confirm(side(moverIsWhite) + ", request to take back " + last + "?",
                            "Takeback · " + side(moverIsWhite))
                        && confirm(side(!moverIsWhite) + ", agree to undo " + last + "?",
                            "Takeback · " + side(!moverIsWhite));
            if (accepted) {
                session.takeBack();
                changed();
            }
            return null;
        });
    }

    private <T> T withPause(Supplier<T> operation) {
        modalDepth++;
        syncActivity();
        try {
            return operation.get();
        } finally {
            modalDepth--;
            syncActivity();
        }
    }

    private boolean activeLive() {
        return screen == Screen.GAME && !closing && !busy && modalDepth == 0 && replay == null
                && session.live().result() == ChessMatch.Result.ACTIVE;
    }

    private void pauseActivity() {
        cancelSearch();
        clearHint();
        boolean running = session.clock.running();
        session.clock.pause();
        if (session.adjudicateTime()) files.autosave(session, true);
        else if (running) files.checkpoint(session);
    }

    private void syncActivity() {
        if (closing) return;
        if (activeLive()) {
            session.clock.resume(session.live().whiteTurn());
            if (session.computerTurn() && search == null) startSearch();
        } else {
            pauseActivity();
        }
        refresh();
    }

    private void tick() {
        if (activeLive() && session.adjudicateTime()) {
            changed();
            return;
        }
        if (activeLive() && session.clock.timed() && System.nanoTime() - lastCheckpoint >= 5_000_000_000L) {
            lastCheckpoint = System.nanoTime();
            files.checkpoint(session);
        }
        refreshClocks();
    }

    private void requestHint() {
        if (!activeLive() || hintWorker != null || !HintRequest.available(session)) return;
        HintRequest request = HintRequest.capture(session);
        suggestedMove = null;
        hintMessage = "Finding an engine suggestion…";
        hintWorker = new SwingWorker<>() {
            @Override protected Move doInBackground() {
                return ComputerSearch.choose(request.snapshot(), ComputerSearch.Level.STANDARD, this::isCancelled);
            }

            @Override protected void done() {
                if (hintWorker != this) return;
                hintWorker = null;
                hintMessage = null;
                try {
                    Move move = get();
                    if (request.accepts(session, activeLive(), move)) {
                        suggestedMove = move;
                        hintMessage = "Engine suggests " + request.position().sanStem(move)
                                + " · " + move.from() + " → " + move.to();
                    }
                } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException error) {
                    hintMessage = "A suggestion is unavailable.";
                }
                refresh();
            }
        };
        hintWorker.execute();
        refresh();
    }

    private void clearHint() {
        SwingWorker<Move, Void> previous = hintWorker;
        hintWorker = null;
        suggestedMove = null;
        hintMessage = null;
        if (previous != null) previous.cancel(true);
    }

    private void startSearch() {
        MatchSession expected = session;
        ChessMatch snapshot = expected.live().searchCopy();
        search = new SwingWorker<>() {
            @Override protected Move doInBackground() {
                return ComputerSearch.choose(snapshot, expected.setup.level(), this::isCancelled);
            }

            @Override protected void done() {
                if (search != this || session != expected || !activeLive() || !session.computerTurn()) return;
                search = null;
                Move move;
                try {
                    move = get();
                } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                    return;
                } catch (ExecutionException error) {
                    move = session.live().searchMoves().stream().findFirst().orElse(null);
                }
                if (move != null) playLive(move);
                else refresh();
            }
        };
        search.execute();
    }

    private void cancelSearch() {
        SwingWorker<Move, Void> previous = search;
        search = null;
        if (previous != null) previous.cancel(true);
    }

    private void playLive(Move move) {
        ChessMatch.Result before = session.live().result();
        if (session.play(move) || session.live().result() != before) changed();
        else syncActivity();
    }

    private boolean confirm(String message, String title) {
        Object[] options = {"Yes", "Cancel"};
        return JOptionPane.showOptionDialog(frame, message, title, JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE, null, options, options[1]) == 0;
    }

    private static String side(boolean white) { return white ? "White" : "Black"; }

    private int currentPly() { return replay == null ? session.live().history().size() : replayPly; }

    private void goTo(int ply) {
        if (screen != Screen.GAME || busy) return;
        List<Move> played = session.live().history();
        if (played.isEmpty() || ply < 0 || ply > played.size()) return;
        if (replay != null && ply == replayPly) return;
        if (replay == null) pauseActivity();
        if (replay == null || ply != replayPly + 1 || !replay.play(played.get(ply - 1))) {
            replay = session.game().atPly(ply);
        }
        replayPly = ply;
        selected = null;
        refresh();
    }

    private void returnLive() {
        if (screen != Screen.GAME || busy || replay == null) return;
        replay = null;
        selected = null;
        syncActivity();
    }

    private void flip() {
        if (screen == Screen.PRACTICE) practiceFlipped = !practiceFlipped;
        else if (screen == Screen.GAME) flipped = !flipped;
        refresh();
    }

    private void resetSelection() {
        selected = null;
        refresh();
    }

    private boolean boardFlipped() { return screen == Screen.PRACTICE ? practiceFlipped : flipped; }

    private Position at(int row, int col) {
        boolean reverse = boardFlipped();
        return new Position(reverse ? 7 - row : row, reverse ? 7 - col : col);
    }

    private void focusBoard() {
        if (screen != Screen.HOME) focusSquare(squares[boardFlipped() ? 1 : 6][boardFlipped() ? 3 : 4]);
    }

    private void focusSquare(Square square) {
        if (boardFocus != square) {
            square.setFocusable(true);
            Square previousFocus = boardFocus;
            boardFocus = square;
            square.requestFocusInWindow();
            previousFocus.setFocusable(false);
        } else {
            square.requestFocusInWindow();
        }
    }

    private void click(Position square) {
        boolean training = screen == Screen.PRACTICE;
        if (busy || (training ? practice.solved() : !activeLive() || session.computerTurn())) return;
        ChessMatch match = training ? practice.board() : session.live();
        Piece piece = match.pieceAt(square);
        if (square.equals(selected)) {
            resetSelection();
        } else if (piece != null && piece.isWhite() == match.whiteTurn()) {
            selected = square;
            refresh();
        } else if (selected != null) {
            List<Move> options = match.legalMovesFrom(selected).stream().filter(move -> move.to().equals(square)).toList();
            if (options.isEmpty()) {
                if (training) puzzleFeedback.setText("Choose a highlighted square.");
                else hint.setText("Choose a highlighted square.");
                return;
            }
            Move move = options.getFirst();
            if (move.promotion() == null) {
                submitMove(move, training);
            } else {
                withPause(() -> {
                    if (match.result() != ChessMatch.Result.ACTIVE) return null;
                    String[] choices = {"Queen", "Rook", "Bishop", "Knight"};
                    int choice = JOptionPane.showOptionDialog(frame, "Choose the new piece for " + square + ".",
                            "Promote pawn", JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null,
                            choices, choices[0]);
                    if (choice >= 0) submitMove(new Move(move.from(), move.to(), Promotion.values()[choice]), training);
                    return null;
                });
            }
        }
    }

    private void submitMove(Move move, boolean training) {
        if (training) {
            practice.tryMove(move);
            resetSelection();
        } else {
            playLive(move);
        }
    }

    private void refresh() {
        boolean training = screen == Screen.PRACTICE;
        ChessMatch shown = training ? practice.board() : replay == null ? session.live() : replay;
        List<Move> played = shown.history();
        List<Move> legal = (training && !practice.solved() || activeLive() && !session.computerTurn())
                ? shown.legalMovesFrom(selected) : List.of();
        Move last = played.isEmpty() ? null : played.getLast();
        Position checked = shown.checkedKing();
        var palette = VisualStyle.palette();
        for (Square[] row : squares) {
            for (Square square : row) {
                Position pos = at(square.row, square.col);
                square.piece = shown.pieceAt(pos);
                square.pos = pos;
                square.legal = legal.stream().anyMatch(move -> move.to().equals(pos));
                boolean light = (square.row + square.col) % 2 == 0;
                square.tile = light ? palette.lightSquare() : palette.darkSquare();
                square.last = last != null && (pos.equals(last.from()) || pos.equals(last.to()));
                square.checked = pos.equals(checked);
                square.selected = pos.equals(selected);
                square.suggested = !training && suggestedMove != null
                        && (pos.equals(suggestedMove.from()) || pos.equals(suggestedMove.to()));
                if (square.last) square.tile = light ? palette.lastLight() : palette.lastDark();
                if (square.checked) square.tile = CHECK;
                if (square.selected) square.tile = palette.selection();
                square.getAccessibleContext().setAccessibleName(pos + ", " + description(square.piece)
                        + (square.selected ? ", selected" : "") + (square.checked ? ", in check" : "")
                        + (square.legal ? ", legal destination" : "") + (square.suggested ? ", engine suggestion" : ""));
                square.repaint();
            }
        }

        hintButton.setEnabled(activeLive() && hintWorker == null && HintRequest.available(session));
        if (training) {
            backToGame.setText(hasGame ? "Back to game" : "Back to home");
            puzzleTitle.setText(practice.puzzle().title());
            puzzleNumber.setText("PRACTICE  ·  " + practice.counter());
            puzzleGoal.setText("Checkmate in " + practice.puzzle().mateIn());
            puzzleFeedback.setText(practice.feedback());
            return;
        }

        List<String> notation = session.live().notation();
        int total = notation.size();
        String liveMode = session.versusComputer() ? "Vs computer  ·  " + session.setup.level().label : "Local two-player";
        mode.setText(busy ? "Opening PGN…" : replay == null ? liveMode : "Replay  ·  " + replayPly + " / " + total);
        String statusText = shown.status();
        Font statusFont = status.getFont().deriveFont(statusSize);
        int availableStatusWidth = gameSidebar.getPreferredSize().width - 26;
        while (statusFont.getSize() > 14 && status.getFontMetrics(statusFont).stringWidth(statusText) > availableStatusWidth) {
            statusFont = statusFont.deriveFont((float) (statusFont.getSize() - 1));
        }
        status.setFont(statusFont);
        status.setText(statusText);
        if (busy) hint.setText("Validating the file and its moves.");
        else if (replay != null) hint.setText("Board and clocks paused.");
        else if (modalDepth > 0) hint.setText("Live game paused.");
        else if (search != null) hint.setText("Computer is thinking…");
        else if (hintMessage != null) hint.setText(hintMessage);
        else if (shown.result() != ChessMatch.Result.ACTIVE) hint.setText("Replay or save the finished game.");
        else if (shown.canClaimDraw()) hint.setText("A draw can be claimed.");
        else if (selected != null) hint.setText("Selected " + selected + (legal.isEmpty() ? " · No legal moves" : " · Esc clears"));
        else hint.setText("Select a piece to see legal moves.");
        status.setToolTipText(statusText);
        boolean editable = screen == Screen.GAME && !busy && modalDepth == 0 && replay == null;
        draw.setEnabled(editable && !session.computerTurn() && shown.canClaimDraw());
        resign.setEnabled(editable && !session.computerTurn() && shown.result() == ChessMatch.Result.ACTIVE);
        takeback.setEnabled(editable && session.takebackTarget() >= 0);
        renderCaptures(whiteCaptures, shown.capturedBy(true));
        renderCaptures(blackCaptures, shown.capturedBy(false));
        refreshClocks();
        first.setEnabled(!busy && total > 0 && currentPly() > 0);
        previous.setEnabled(!busy && currentPly() > 0);
        next.setEnabled(!busy && replay != null && replayPly < total);
        live.setEnabled(!busy && replay != null);
        moveList.setEnabled(!busy);

        updatingHistory = true;
        try {
            if (!renderedMoves.equals(notation) || !renderedStart.equals(session.game().start())) {
                renderedMoves = notation;
                renderedStart = session.game().start();
                history.clear();
                for (int i = 0; i < total; i++) history.addElement(historyRow(notation, i));
            }
            if (replay == null || replayPly == 0) moveList.clearSelection();
            else moveList.setSelectedIndex(replayPly - 1);
            if (total > 0) moveList.ensureIndexIsVisible(replay == null ? total - 1 : Math.max(0, replayPly - 1));
        } finally {
            updatingHistory = false;
        }
    }

    private void refreshClocks() {
        boolean active = activeLive();
        whitePlayer.setText(active && session.live().whiteTurn() ? "▸ White" : "White");
        blackPlayer.setText(active && !session.live().whiteTurn() ? "▸ Black" : "Black");
        if (!session.clock.timed()) {
            whiteClock.setText("Untimed");
            blackClock.setText("Untimed");
            whiteClock.setForeground(VisualStyle.palette().muted());
            blackClock.setForeground(VisualStyle.palette().muted());
            return;
        }
        MatchClock.Time time = replay == null ? session.clock.reading() : session.clock.atPly(replayPly);
        setClock(whiteClock, time.white(), true);
        setClock(blackClock, time.black(), false);
    }

    private void setClock(JLabel label, long nanos, boolean white) {
        long seconds = (nanos + 999_999_999L) / 1_000_000_000L;
        label.setText(String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60));
        label.setForeground(nanos == 0 ? VisualStyle.palette().danger() : VisualStyle.palette().ink());
        label.getAccessibleContext().setAccessibleName(side(white) + " clock: " + label.getText());
    }

    private static void renderCaptures(JLabel label, String captured) {
        if (captured.isEmpty()) {
            label.setIcon(null);
            label.setText("—");
            label.setToolTipText("No pieces captured");
            label.getAccessibleContext().setAccessibleName("No pieces captured");
            return;
        }
        StringBuilder compact = new StringBuilder();
        for (char letter : "QRBNP".toCharArray()) {
            long count = captured.chars().filter(piece -> Character.toUpperCase(piece) == letter).count();
            if (count > 0) {
                if (!compact.isEmpty()) compact.append(' ');
                compact.append(letter);
                if (count > 1) compact.append('×').append(count);
            }
        }
        label.setText("");
        String[] groups = compact.toString().split(" ");
        label.setIcon(new Icon() {
            @Override public int getIconWidth() { return groups.length * 29; }
            @Override public int getIconHeight() { return 20; }
            @Override public void paintIcon(Component component, Graphics graphics, int x, int y) {
                Graphics2D g = (Graphics2D) graphics.create();
                for (String group : groups) {
                    PieceArt.paint(g, x, y, 20, group.charAt(0), Character.isUpperCase(captured.charAt(0)));
                    if (group.length() > 1) {
                        g.setColor(VisualStyle.palette().ink());
                        g.setFont(component.getFont().deriveFont(10f));
                        g.drawString(group.substring(2), x + 18, y + 15);
                    }
                    x += 29;
                }
                g.dispose();
            }
        });
        label.setToolTipText("Captured pieces: " + compact);
        label.getAccessibleContext().setAccessibleName("Captured pieces: " + compact);
    }

    private String historyRow(List<String> notation, int index) {
        return String.format(Locale.ROOT, "%-6s %s", session.game().moveLabel(index), notation.get(index));
    }

    private static String description(Piece piece) {
        return piece == null ? "empty" : (piece.isWhite() ? "white " : "black ")
                + piece.getClass().getSimpleName().toLowerCase(Locale.ROOT);
    }

    private static JButton button(String text, String description) {
        JButton button = new JButton(text);
        // Let the same UI delegate choose the foreground, background, border,
        // and focus treatment, including when the control is disabled.
        button.setToolTipText(description);
        button.getAccessibleContext().setAccessibleDescription(description);
        return button;
    }

    private static void bind(JComponent component, int key, int modifiers, String name, Runnable action) {
        bind(component, JComponent.WHEN_IN_FOCUSED_WINDOW, KeyStroke.getKeyStroke(key, modifiers), name, action);
    }

    private static void bind(JComponent component, int condition, KeyStroke key, String name, Runnable action) {
        component.getInputMap(condition).put(key, name);
        component.getActionMap().put(name, new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent event) { action.run(); }
        });
    }

    private final class Square extends JButton {
        private static final long serialVersionUID = 1L;
        private final int row;
        private final int col;
        private transient Position pos;
        private transient Piece piece;
        private Color tile;
        private boolean legal;
        private boolean last;
        private boolean checked;
        private boolean selected;
        private boolean suggested;

        Square(int row, int col) {
            this.row = row;
            this.col = col;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusable(false);
            addActionListener(event -> {
                focusSquare(this);
                click(at(row, col));
            });
            bind(this, WHEN_FOCUSED, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "square-enter", this::doClick);
            int[] keys = {KeyEvent.VK_UP, KeyEvent.VK_DOWN, KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT};
            int[][] steps = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
            for (int i = 0; i < keys.length; i++) {
                int r = row + steps[i][0];
                int c = col + steps[i][1];
                if (r >= 0 && r < 8 && c >= 0 && c < 8) {
                    bind(this, WHEN_FOCUSED, KeyStroke.getKeyStroke(keys[i], 0), "square-" + keys[i],
                            () -> focusSquare(squares[r][c]));
                }
            }
        }

        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                int size = Math.min(getWidth(), getHeight());
                if (size <= 0) return;
                boolean high = VisualStyle.appearance() == VisualStyle.Appearance.HIGH_CONTRAST;
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(tile == null ? VisualStyle.palette().lightSquare() : tile);
                g.fillRect(0, 0, size, size);
                if (last) {
                    g.setColor(new Color(0x695521));
                    int corner = Math.max(6, size / 8);
                    g.fillPolygon(new int[]{size, size - corner, size}, new int[]{0, 0, corner}, 3);
                    g.fillPolygon(new int[]{0, corner, 0}, new int[]{size, size, size - corner}, 3);
                }
                if (selected || checked) {
                    g.setColor(checked ? new Color(0x8B281A) : DARK_MARK);
                    g.setStroke(new BasicStroke(2.6f));
                    g.drawRect(2, 2, size - 5, size - 5);
                    if (checked) {
                        g.setStroke(new BasicStroke(1));
                        g.drawRect(6, 6, size - 13, size - 13);
                    }
                }
                if (legal) {
                    int dot = Math.max(9, size / 5);
                    Shape marker = piece == null
                            ? new java.awt.geom.Ellipse2D.Double((size - dot) / 2.0, (size - dot) / 2.0, dot, dot)
                            : new java.awt.geom.Ellipse2D.Double(5, 5, size - 10, size - 10);
                    if (piece == null) {
                        g.setColor(DARK_MARK);
                        g.fill(marker);
                    }
                    g.setColor(high ? Color.WHITE : DARK_MARK);
                    g.setStroke(new BasicStroke(piece == null ? 1.5f : 2.5f));
                    g.draw(marker);
                    if (high && piece != null) {
                        g.setColor(DARK_MARK);
                        g.setStroke(new BasicStroke(1));
                        g.draw(marker);
                    }
                }
                if (piece != null) PieceArt.paint(g, size * .056, size * .081, size * .888,
                        piece.toString().charAt(0), piece.isWhite());
                if (pos != null) {
                    int font = Math.min(16, Math.max(11, size / 6));
                    g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, font));
                    int luminance = (tile.getRed() * 299 + tile.getGreen() * 587 + tile.getBlue() * 114) / 1000;
                    g.setColor(luminance < 135 ? Color.WHITE : DARK_MARK);
                    if (col == 0) g.drawString(Integer.toString(8 - pos.getRow()), 5, font + 4);
                    if (row == 7) g.drawString(String.valueOf((char) ('a' + pos.getCol())), size - font - 2, size - 5);
                }
                if (suggested) {
                    g.setStroke(new BasicStroke(5));
                    g.setColor(Color.WHITE);
                    g.drawRoundRect(7, 7, size - 15, size - 15, 10, 10);
                    g.setStroke(new BasicStroke(2.5f));
                    g.setColor(new Color(0x274BA5));
                    g.drawRoundRect(7, 7, size - 15, size - 15, 10, 10);
                }
                if (isFocusOwner()) {
                    g.setStroke(new BasicStroke(3));
                    g.setColor(Color.WHITE);
                    g.drawRect(4, 4, size - 9, size - 9);
                    g.setStroke(new BasicStroke(1.7f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                            10f, new float[]{3, 3}, 0));
                    g.setColor(DARK_MARK);
                    g.drawRect(4, 4, size - 9, size - 9);
                }
            } finally {
                g.dispose();
            }
        }

    }
}
