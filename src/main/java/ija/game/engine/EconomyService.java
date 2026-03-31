package ija.game.engine;

import ija.game.model.Building;
import ija.game.model.GameMap;
import ija.game.model.GameState;
import ija.game.model.Tile;

public class EconomyService {

    public int applyStartTurnIncome(GameState state) {
        int playerId = state.getCurrentPlayerId();
        int income = 0;

        GameMap map = state.getMap();
        for (int y = 0; y < map.getHeight(); y++) {
            for (int x = 0; x < map.getWidth(); x++) {
                Tile tile = map.getTile(x, y);
                Building building = tile.getRawBuilding();
                if (building == null) {
                    continue;
                }
                if (building.getOwnerId() != playerId) {
                    continue;
                }
                if (!building.getType().generatesIncome()) {
                    continue;
                }
                income += tile.getTerrain().getIncome();
            }
        }

        state.getCurrentPlayer().addFunds(income);
        return income;
    }
}
