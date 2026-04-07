/**
 * Authors: Team xrepcim00
 * Description: Handles factory purchases with ownership, occupancy, and funds validation.
 */
package ija.game.engine;

import ija.game.model.building.Building;
import ija.game.model.map.GameMap;
import ija.game.model.map.Position;
import ija.game.model.map.Tile;
import ija.game.model.player.Player;
import ija.game.model.state.GameState;
import ija.game.model.unit.UnitFactory;
import ija.game.model.unit.UnitType;

public class PurchaseService {

    public record PurchaseOutcome(boolean success, String message) {
        public static PurchaseOutcome ok(String message) { return new PurchaseOutcome(true, message); }
        public static PurchaseOutcome fail(String message) { return new PurchaseOutcome(false, message); }
    }

    public PurchaseOutcome buy(GameState state, Position factoryPos, UnitType type) {
        GameMap map = state.getMap();
        if (factoryPos == null)
            return PurchaseOutcome.fail("Click a factory tile first.");
        if (!map.isInBounds(factoryPos))
            return PurchaseOutcome.fail("Selected tile is out of bounds.");

        Tile tile = map.getTile(factoryPos);
        Building building = tile.getRawBuilding();
        if (building == null)
            return PurchaseOutcome.fail("No building on selected tile.");
        if (!building.getType().allowsPurchase())
            return PurchaseOutcome.fail("Selected building is not a factory.");
        if (building.getOwnerId() != state.getCurrentPlayerId())
            return PurchaseOutcome.fail("Factory is not owned by current player.");
        if (tile.hasUnit())
            return PurchaseOutcome.fail("Factory tile is occupied.");

        Player player = state.getCurrentPlayer();
        int cost = type.getBuyCost();
        if (!player.canAfford(cost))
            return PurchaseOutcome.fail("Not enough funds for " + type + ".");

        player.spendFunds(cost);
        var unit = UnitFactory.create(type, player.getId());
        unit.setHasMoved(true);
        unit.setHasActed(true);
        tile.setUnit(unit);
        return PurchaseOutcome.ok("Bought " + type + " for " + cost + " at " + factoryPos + ".");
    }
}
