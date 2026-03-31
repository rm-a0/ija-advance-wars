package ija.game.engine;

import ija.game.model.CombatResolver;
import ija.game.model.GameMap;
import ija.game.model.GameState;
import ija.game.model.Position;
import ija.game.model.Tile;
import ija.game.model.Unit;
import ija.game.model.UnitType;

public class CombatService {

    public CombatResolver.CombatResult attack(GameState state, Position attackerPos, Position defenderPos) {
        GameMap map = state.getMap();
        if (!map.isInBounds(attackerPos) || !map.isInBounds(defenderPos)) {
            throw new IllegalArgumentException("Attack position out of bounds");
        }

        Tile attackerTile = map.getTile(attackerPos);
        Tile defenderTile = map.getTile(defenderPos);

        Unit attacker = attackerTile.getUnit().orElseThrow(() -> new IllegalStateException("No attacker unit"));
        Unit defender = defenderTile.getUnit().orElseThrow(() -> new IllegalStateException("No defender unit"));

        validateOwnershipAndTurn(state, attacker, defender);

        int distance = attackerPos.manhattanDistance(defenderPos);
        validateAttackRange(attacker, distance);

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

        return result;
    }

    private void validateOwnershipAndTurn(GameState state, Unit attacker, Unit defender) {
        if (attacker.getPlayerId() != state.getCurrentPlayerId()) {
            throw new IllegalStateException("Attacker does not belong to current player");
        }
        if (defender.getPlayerId() == attacker.getPlayerId()) {
            throw new IllegalStateException("Cannot attack friendly unit");
        }
        if (attacker.getHasActed()) {
            throw new IllegalStateException("Unit already acted this turn");
        }
    }

    private void validateAttackRange(Unit attacker, int distance) {
        if (!attacker.getType().canAttackAt(distance)) {
            throw new IllegalStateException("Target not in attack range");
        }
        if (attacker.getType() == UnitType.ARTILLERY && attacker.getHasMoved()) {
            throw new IllegalStateException("Artillery cannot move and attack in the same turn");
        }
    }
}
