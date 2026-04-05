/**
 * Authors: Team xrepcim00
 * Description: Records game actions as ordered state snapshots for replay.
 */
package ija.game.io;

import ija.game.model.state.GameState;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

public class GameLogService {

    private final boolean enabled;
    private final GamePersistence persistence;
    private final Path sessionDir;

    private int sequence;

    private GameLogService(boolean enabled, Path sessionDir) {
        this.enabled = enabled;
        this.persistence = new GamePersistence();
        this.sessionDir = sessionDir;
    }

    public static GameLogService disabled() {
        return new GameLogService(false, null);
    }

    // Starts a new logging session by creating a unique directory and recording the initial game state.
    public static GameLogService startSession(Path logRoot, GameState initialState) {
        Path sessionDir = createSessionDirectory(logRoot);
        GameLogService service = new GameLogService(true, sessionDir);
        service.record("START", initialState);
        return service;
    }

    // Records a game state snapshot with an associated action description, if logging is enabled.
    public void record(String action, GameState state) {
        if (!enabled)
            return;

        sequence++;
        Path snapshotPath = sessionDir.resolve(String.format("%05d_%s.json", sequence, sanitize(action)));
        persistence.saveGame(state, snapshotPath);
    }

    public Path getSessionDir() {
        return sessionDir;
    }

    // Creates a new session directory with a timestamped name under the specified log root, ensuring it exists.
    private static Path createSessionDirectory(Path logRoot) {
        try {
            Files.createDirectories(logRoot);
            String sessionName = "session-" + Instant.now().toString().replace(':', '-');
            Path sessionDir = logRoot.resolve(sessionName);
            Files.createDirectories(sessionDir);
            return sessionDir;
        } catch (IOException e) {
            throw new RuntimeException("Failed to create game log directory under: " + logRoot, e);
        }
    }

    // Sanitizes the action description to create a safe filename
    private static String sanitize(String action) {
        String value = action == null ? "event" : action.toLowerCase();
        return value.replaceAll("[^a-z0-9_-]+", "_");
    }
}