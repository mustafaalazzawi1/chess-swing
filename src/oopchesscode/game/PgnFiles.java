package oopchesscode.game;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

final class PgnFiles {
    static final int LIMIT = 512 * 1024;

    private PgnFiles() {}

    static Pgn.Game read(Path path) throws IOException {
        if (!Files.isRegularFile(path)) throw new IOException("Choose a regular PGN file.");
        byte[] bytes;
        try (InputStream input = Files.newInputStream(path)) {
            bytes = input.readNBytes(LIMIT + 1);
        }
        if (bytes.length > LIMIT) throw new IOException("PGN files must be 512 KiB or smaller.");
        try {
            return Pgn.parse(StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(bytes)).toString());
        } catch (CharacterCodingException e) {
            throw new IOException("This file is not valid UTF-8. Save it as UTF-8 PGN and try again.", e);
        }
    }

    static void write(Path path, Pgn.Game game) throws IOException {
        Path target = path.toAbsolutePath().normalize();
        Path parent = target.getParent();
        byte[] bytes = Pgn.write(game).getBytes(StandardCharsets.UTF_8);
        if (bytes.length > LIMIT) throw new IOException("The exported PGN exceeds 512 KiB.");
        if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)
                && !Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("The destination must be a regular file.");
        }

        Path temporary = Files.createTempFile(parent, ".chess-swing-", ".tmp");
        try {
            Files.write(temporary, bytes);
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
