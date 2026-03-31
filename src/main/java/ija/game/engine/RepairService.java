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
            if (!isRepairable(unit, tile, playerId)) {
                continue;
            }

            int missingHp = unit.getType().getMaxHp() - unit.getHp();
            int healAmount = Math.min(20, missingHp);
            int repairCost = calculateRepairCost(unit, healAmount);

            if (!player.canAfford(repairCost)) {
                continue;
            }

            player.spendFunds(repairCost);
            unit.heal(healAmount);
            repairedUnits++;
        }

        return repairedUnits;
    }

    private boolean isRepairable(Unit unit, Tile tile, int playerId) {
        Building building = tile.getRawBuilding();
        return building != null
            && building.getOwnerId() == playerId
            && building.getType().heals()
            && unit.getHp() < unit.getType().getMaxHp();
    }

    private int calculateRepairCost(Unit unit, int healAmount) {
        int costPer10 = unit.getType().getBuyCost() / 10;
        int blocksOf10 = (int) Math.ceil(healAmount / 10.0);
        return costPer10 * blocksOf10;
    }
}
