package ija.game.engine;

import ija.game.model.Building;
import ija.game.model.GameMap;
import ija.game.model.GameState;
import ija.game.model.Position;
import ija.game.model.Tile;
import ija.game.model.Unit;

public class RepairService {

    public int applyStartTurnRepairs(GameState state) {
        int playerId = state.getCurrentPlayerId();
        int repairedUnits = 0;

        GameMap map = state.getMap();
        var player = state.getCurrentPlayer();

        for (var unitAt : map.getUnitsForPlayer(playerId)) {
            Unit unit = unitAt.unit();
            Position pos = unitAt.pos();
            Tile tile = map.getTile(pos);
            Building building = tile.getRawBuilding();
            if (building == null) {
                continue;
            }
            if (building.getOwnerId() != playerId || !building.getType().heals()) {
                continue;
            }
            if (unit.getHp() >= unit.getType().getMaxHp()) {
                continue;
            }

            int missingHp = unit.getType().getMaxHp() - unit.getHp();
            int healAmount = Math.min(20, missingHp);

            int costPer10 = unit.getType().getBuyCost() / 10;
            int blocksOf10 = (int) Math.ceil(healAmount / 10.0);
            int repairCost = costPer10 * blocksOf10;

            if (!player.canAfford(repairCost)) {
                continue;
            }

            player.spendFunds(repairCost);
            unit.heal(healAmount);
            repairedUnits++;
        }

        return repairedUnits;
    }
}
