/**
 * Authors: Team xrepcim00
 * Description: Validates combat actions and resolves attack/counterattack outcomes.
 */
package ija.game.engine;

import ija.game.model.unit.CombatResolver;
import ija.game.model.map.GameMap;
import ija.game.model.state.GameState;
import ija.game.model.map.Position;
import ija.game.model.map.Tile;
import ija.game.model.unit.Unit;
import ija.game.model.unit.UnitType;

public class CombatService {

    public record AttackOutcome(boolean success, CombatResolver.CombatResult result, String message) {
        public static AttackOutcome ok(CombatResolver.CombatResult result) {
            return new AttackOutcome(true, result, "");
        }

        public static AttackOutcome fail(String message) {
            return new AttackOutcome(false, null, message);
        }
    }

    public AttackOutcome attack(GameState state, Position attackerPos, Position defenderPos) {
        GameMap map = state.getMap();
        if (!map.isInBounds(attackerPos) || !map.isInBounds(defenderPos))
            return AttackOutcome.fail("Attack position out of bounds");

        Tile attackerTile = map.getTile(attackerPos);
        Tile defenderTile = map.getTile(defenderPos);

        Unit attacker = attackerTile.getUnit().orElse(null);
        Unit defender = defenderTile.getUnit().orElse(null);

        String validationError = validateAttack(state, attacker, defender, attackerPos, defenderPos);
        if (validationError != null)
            return AttackOutcome.fail(validationError);

        int distance = attackerPos.manhattanDistance(defenderPos);

        CombatResolver.CombatResult result = CombatResolver.resolve(
            attacker,
            defender,
            attackerTile.getTerrain(),
            defenderTile.getTerrain(),
            distance
        );

        attacker.setHasActed(true);

        if (!defender.isAlive()) {
            map.removeUnit(defenderPos);
        }
        if (!attacker.isAlive()) {
            map.removeUnit(attackerPos);
        }

        return AttackOutcome.ok(result);
    }

    private String validateAttack(GameState state, Unit attacker, Unit defender, Position attackerPos, Position defenderPos) {
        if (attacker == null)
            return "No attacker unit";
        if (defender == null)
            return "No defender unit";
        if (attacker.getPlayerId() != state.getCurrentPlayerId())
            return "Attacker does not belong to current player";
        if (defender.getPlayerId() == attacker.getPlayerId())
            return "Cannot attack friendly unit";
        if (attacker.getHasActed())
            return "Unit already acted this turn";

        int distance = attackerPos.manhattanDistance(defenderPos);
        if (!attacker.getType().canAttackAt(distance))
            return "Target not in attack range";
        if (attacker.getType() == UnitType.ARTILLERY && attacker.getHasMoved())
            return "Artillery cannot move and attack in the same turn";
        return null;
    }
}
