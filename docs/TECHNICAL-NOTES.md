# Chess Swing interview notes

## Rules and state

- Trace a move from a square click through `ChessWindow`, `MatchSession`, and `ChessMatch.play`. Piece geometry produces candidates. The engine simulates a candidate, checks king safety, and restores the temporary board before deciding whether to commit.
- The position key includes board occupancy, turn, castling rights, and an en passant square only when a legal capture is available. Move-count and repetition rules depend on this state, even if two boards appear identical.
- SAN is computed from the pre-move position. If another legal same-type piece can reach the destination, record the minimum file/rank disambiguation. Check and mate suffixes come from the resulting position.
- Takebacks and replay rebuild from the validated starting position and a prefix of moves. This avoids maintaining multiple inverse-move paths for castling, promotion, and en passant. Backward replay takes longer for very long games.

## Computer opponent

- Negamax expresses both sides using one score from the current player's perspective. After making a move, negate the child's score. Alpha-beta skips branches that cannot alter the decision. Every branch uses the same chess rules as a human move.
- Evaluation values are pawn 100, knight 320, bishop 330, rook 500, and queen 900. Center proximity is measured with Manhattan distance from the center; knights and bishops get three times the center bonus, rooks and queens one times it, and pawns add a three-point bonus per advanced rank. Kings have no positional bonus. Terminal mate scores dominate material, and terminal draws score zero.
- Iterative deepening saves a result from the last finished depth. Time, node, and cancellation checks keep the search bounded; direct captures and promotions are ordered first. Copying positions makes isolation approachable at this scale and uses more allocations than make/unmake search.
- Swing retains separate worker references for computer play and on-demand hints. New game, import, replay, takeback, dialog, or close can invalidate it. The worker sees a detached board. Its EDT completion handler checks reference and session identity and revalidates turn, flag fall, and legality before committing.

## Learning features

- A hint requests the same bounded search on a detached position and renders the returned move without committing it. The completion callback checks worker identity, session, match identity, ply, live view, side, flag fall, and final move legality. A takeback can reproduce the same position while replacing the match identity, so a ply count alone is insufficient.
- A mate-in-two attack qualifies only if every legal defense has at least one mating response. The tests independently enumerate attacks, defenses, and mating replies. Practice selects a defense with the fewest mating continuations, and accepts alternate correct attacks and finishes.
- Home and practice suspend live activity. Practice owns only its own puzzle board and feedback. Returning to a game restores the paused live session and does not commit puzzle moves.

## Time and persistence

- The clock uses `System.nanoTime()`, not wall-clock time. A turn is charged when sampled, and a move snapshots both readings. In tests a fake monotonic clock drives pauses, replay, takebacks, flag fall, and recovery without sleeps.
- PGN is the portable move record. Private recovery additionally stores a version, session setup, and bounded clock checkpoints in local tags. Recovery validates the ledger against the number and color of the moves. A file lock gives one window recovery ownership.
- Play and export a short game, enter replay, and compare the live position afterward. Then describe what should happen if the search completes just as the user enters replay or a clock expires.

## Visual layer

- `VisualStyle` owns the palette and FlatLaf defaults. Theme changes update the component tree with the history selection callback guarded. They do not call the session pause, resume, search or move paths.
- `PieceArt` draws original reusable silhouettes from Java2D paths. The same shapes are used on the scaled board, the Home illustration, and the capture display. Swing buttons retain their standard accessible actions.
- The sidebar derives its width from the current controls' preferred sizes, and the minimum window size accounts for the Home and board layouts. The headful test checks actual button bounds, label fit, and palette replacement.

## Discuss honestly

- The original Java/Swing project is Mustafa's. The later implementation was assisted. Be prepared to explain and modify a specific feature rather than claiming sole authorship.
- This is a modest local engine. Its tests establish behavior for covered cases, not an Elo or comprehensive proof of legality. Linux automated tests and UI checks are separate from the owner's v1.0.1 Mac JAR UI/computer/clock smoke test and v1.0.0 Apple-silicon native build and launch. Hosted Actions and v1.2.0 Mac checks remain pending.
