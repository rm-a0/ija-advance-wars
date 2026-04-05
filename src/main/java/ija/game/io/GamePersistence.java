/**
 * Authors: Team xrepcim00
 * Description: Shared JSON persistence for game state snapshots.
 */
package ija.game.io;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import ija.game.model.state.GameState;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class GamePersistence {

    private final ObjectMapper mapper;

    // Initializes the JSON object mapper with pretty-printing enabled for better readability of saved game states.
    public GamePersistence() {
        this.mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    }

    // Saves the given game state to the specified file path as JSON, ensuring parent directories exist.
    public void saveGame(GameState state, Path filePath) {
        try {
            Path parent = filePath.getParent();
            if (parent != null)
                Files.createDirectories(parent);
            mapper.writeValue(filePath.toFile(), state);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save game: " + filePath, e);
        }
    }

    // Loads a game state from the specified JSON file path.
    public GameState loadGame(Path filePath) {
        try {
            return mapper.readValue(filePath.toFile(), GameState.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load game: " + filePath, e);
        }
    }
}