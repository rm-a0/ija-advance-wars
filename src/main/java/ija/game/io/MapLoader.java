/**
 * Authors: Team xrepcim00
 * Description: Loads map definitions from 
 * JSON files and constructs the initial GameState.
 */
package ija.game.io;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ija.game.model.building.Building;
import ija.game.model.building.BuildingType;
import ija.game.model.map.GameMap;
import ija.game.model.state.GameState;
import ija.game.model.player.Player;
import ija.game.model.map.TerrainType;
import ija.game.model.unit.Unit;
import ija.game.model.unit.UnitType;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MapLoader {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final GamePersistence PERSISTENCE = new GamePersistence();

    public static GameState loadMap(String jsonPath) {
        try {
            File file = new File(jsonPath);
            if (!file.exists() || !file.isFile())
                throw new IllegalArgumentException("Map file not found: " + jsonPath + " (expected a JSON file on disk, e.g. under data/maps/)");

            JsonNode root = MAPPER.readTree(file);

            JsonNode meta = root.get("metadata");
            int width     = meta.get("width").asInt();
            int height    = meta.get("height").asInt();

            GameMap map = new GameMap(width, height);

            // Terrain
            JsonNode grid = root.get("grid");
            for (int y = 0; y < height; y++) {
                JsonNode row = grid.get(y);
                for (int x = 0; x < width; x++) {
                    TerrainType terrain = TerrainType.valueOf(row.get(x).asText());
                    map.getTile(x, y).setTerrain(terrain);
                }
            }

            // Buildings
            for (JsonNode b : root.get("buildings")) {
                int x             = b.get("x").asInt();
                int y             = b.get("y").asInt();
                BuildingType type = BuildingType.valueOf(b.get("type").asText());
                int owner         = b.get("owner").asInt();
                map.getTile(x, y).setBuilding(new Building(type, owner));
            }

            // Units
            for (JsonNode u : root.get("units")) {
                int x         = u.get("x").asInt();
                int y         = u.get("y").asInt();
                UnitType type = UnitType.valueOf(u.get("type").asText());
                int owner     = u.get("owner").asInt();
                int hp        = u.get("hp").asInt();
                map.getTile(x, y).setUnit(new Unit(type, owner, hp));
            }

            // Players
            List<Player> players = new ArrayList<>();
            players.add(new Player(1, "Player 1", 3000, false));
            players.add(new Player(2, "Player 2", 3000, true));

            return new GameState(map, players);

        } catch (IOException e) {
            throw new RuntimeException("Failed to load map: " + jsonPath, e);
        }
    }

    public static void saveGame(GameState state, String filePath) {
        PERSISTENCE.saveGame(state, new File(filePath).toPath());
    }

    public static GameState loadGame(String filePath) {
        return PERSISTENCE.loadGame(new File(filePath).toPath());
    }
}
