/**
 * Authors: Team xrepcim00
 * Description: Calculates and applies turn income from owned income-generating buildings.
 */
package ija.game.engine;

import ija.game.model.building.Building;
import ija.game.model.map.GameMap;
import ija.game.model.state.GameState;
import ija.game.model.map.Tile;

public class EconomyService {

    public int applyStartTurnIncome(GameState state) {
        int playerId = state.getCurrentPlayerId();
        int income = 0;

        GameMap map = state.getMap();
        for (int y = 0; y < map.getHeight(); y++) {
            for (int x = 0; x < map.getWidth(); x++) {
                Tile tile = map.getTile(x, y);
                if (!shouldCountIncome(tile, playerId))
                    continue;
                income += tile.getTerrain().getIncome();
            }
        }

        state.getCurrentPlayer().addFunds(income);
        return income;
    }

    private boolean shouldCountIncome(Tile tile, int playerId) {
        Building building = tile.getRawBuilding();
        return building != null
            && building.getOwnerId() == playerId
            && building.getType().generatesIncome();
    }
}
