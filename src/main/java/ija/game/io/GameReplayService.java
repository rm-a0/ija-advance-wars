/**
 * Authors: Team xrepcim00
 * Description: Loads logged game-state snapshots and allows replay navigation.
 */
package ija.game.io;

import ija.game.model.state.GameState;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class GameReplayService {

    private final GamePersistence persistence;
    private final Path sessionDir;
    private final List<Path> snapshots;

    private int index;

    private GameReplayService(Path sessionDir, List<Path> snapshots) {
        this.persistence = new GamePersistence();
        this.sessionDir = sessionDir;
        this.snapshots = snapshots;
        this.index = Math.max(0, snapshots.size() - 1);
    }

    public static Optional<GameReplayService> loadLatest(Path logRoot) {
        try {
            if (!Files.exists(logRoot))
                return Optional.empty();

            Path latestSession = findLatestSession(logRoot);
            if (latestSession == null)
                return Optional.empty();

            List<Path> snapshots = listSnapshots(latestSession);
            if (snapshots.isEmpty())
                return Optional.empty();

            return Optional.of(new GameReplayService(latestSession, snapshots));
        } catch (IOException e) {
            throw new RuntimeException("Failed to load replay session from: " + logRoot, e);
        }
    }

    public GameState currentState() {
        return persistence.loadGame(snapshots.get(index));
    }

    public GameState previous() {
        if (canPrevious())
            index--;
        return currentState();
    }

    public GameState next() {
        if (canNext())
            index++;
        return currentState();
    }

    public boolean canPrevious() {
        return index > 0;
    }

    public boolean canNext() {
        return index < snapshots.size() - 1;
    }

    public String currentLabel() {
        return (index + 1) + "/" + snapshots.size() + " " + snapshots.get(index).getFileName();
    }

    public Path getSessionDir() {
        return sessionDir;
    }

    public void deleteSession() {
        try {
            if (!Files.exists(sessionDir))
                return;

            List<Path> paths;
            try (var stream = Files.walk(sessionDir)) {
                paths = stream.sorted(Comparator.reverseOrder()).toList();
            }

            for (Path path : paths)
                Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete replay session: " + sessionDir, e);
        }
    }

    private static Path findLatestSession(Path logRoot) throws IOException {
        try (var stream = Files.list(logRoot)) {
            return stream
                .filter(Files::isDirectory)
                .max(Comparator.comparingLong(path -> path.toFile().lastModified()))
                .orElse(null);
        }
    }

    private static List<Path> listSnapshots(Path sessionDir) throws IOException {
        try (var stream = Files.list(sessionDir)) {
            return stream
                .filter(path -> path.getFileName().toString().endsWith(".json"))
                .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                .toList();
        }
    }
}