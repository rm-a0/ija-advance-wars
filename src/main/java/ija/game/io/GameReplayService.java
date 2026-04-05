/**
 * Authors: Team xrepcim00
 * Description: Loads logged game-state snapshots and allows replay navigation.
 */
package ija.game.io;

import ija.game.model.state.GameState;

import java.io.IOException;
import java.util.ArrayDeque;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Optional;

public class GameReplayService {

    private interface ReplayCommand {
        GameState execute();
        GameState undo();
    }

    // Command implementation for stepping between snapshots in the replay history.
    private final class SnapshotStepCommand implements ReplayCommand {
        private final int fromIndex;
        private final int toIndex;

        private SnapshotStepCommand(int fromIndex, int toIndex) {
            this.fromIndex = fromIndex;
            this.toIndex = toIndex;
        }

        // Executes the command by moving to the target snapshot index and returning the corresponding game state.
        @Override
        public GameState execute() {
            index = toIndex;
            return currentState();
        }

        // Undoes the command by moving back to the original snapshot index and returning the corresponding game state.
        @Override
        public GameState undo() {
            index = fromIndex;
            return currentState();
        }
    }

    private final GamePersistence persistence;
    private final Path sessionDir;
    private final List<Path> snapshots;
    private final Deque<ReplayCommand> undoStack;
    private final Deque<ReplayCommand> redoStack;

    private int index;

    // Init the replay service with the given session directory and list of snapshot paths.
    private GameReplayService(Path sessionDir, List<Path> snapshots) {
        this.persistence = new GamePersistence();
        this.sessionDir = sessionDir;
        this.snapshots = snapshots;
        this.undoStack = new ArrayDeque<>();
        this.redoStack = new ArrayDeque<>();
        this.index = Math.max(0, snapshots.size() - 1);
        seedUndoHistory();
    }

    // Loads the latest replay session from the specified log root directory.
    public static Optional<GameReplayService> loadLatest(Path logRoot) {
        try {
            if (!Files.exists(logRoot))
                return Optional.empty();

            Optional<Path> latestSession = findLatestSession(logRoot);
            if (latestSession.isEmpty())
                return Optional.empty();

            List<Path> snapshots = listSnapshots(latestSession.get());
            if (snapshots.isEmpty())
                return Optional.empty();

            return Optional.of(new GameReplayService(latestSession.get(), snapshots));
        } catch (IOException e) {
            throw new RuntimeException("Failed to load replay session from: " + logRoot, e);
        }
    }

    public GameState currentState() {
        return persistence.loadGame(snapshots.get(index));
    }

    // Moves to the previous snapshot in the replay history and returns the previous game state.
    public GameState previous() {
        if (!canPrevious())
            return currentState();

        ReplayCommand command = undoStack.pop();
        GameState state = command.undo();
        redoStack.push(command);
        return state;
    }

    // Moves to the next snapshot in the replay history and returns the next game state.
    public GameState next() {
        if (!canNext())
            return currentState();

        ReplayCommand command = redoStack.pop();
        GameState state = command.execute();
        undoStack.push(command);
        return state;
    }

    public boolean canPrevious() {
        return !undoStack.isEmpty();
    }

    public boolean canNext() {
        return !redoStack.isEmpty();
    }

    public String currentLabel() {
        return (index + 1) + "/" + snapshots.size() + " " + snapshots.get(index).getFileName();
    }

    public Path getSessionDir() {
        return sessionDir;
    }

    // Deletes the entire replay session directory and all its contents.
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

    // Finds the latest session directory under the specified log root by comparing last modified timestamps.
    private static Optional<Path> findLatestSession(Path logRoot) throws IOException {
        try (var stream = Files.list(logRoot)) {
            return stream
                .filter(Files::isDirectory)
                .max(Comparator.comparingLong(path -> path.toFile().lastModified()));
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

    private void seedUndoHistory() {
        if (snapshots.size() < 2)
            return;

        for (int i = 1; i < snapshots.size(); i++)
            undoStack.push(new SnapshotStepCommand(i - 1, i));
    }
}