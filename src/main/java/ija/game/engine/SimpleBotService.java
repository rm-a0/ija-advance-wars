/**
 * Authors: Team xrepcim00
 * Description: Provides simple AI behavior for bot purchases, movement, and attacks.
 */
package ija.game.engine;

import ija.game.model.map.GameMap;
import ija.game.model.map.Position;
import ija.game.model.state.GameState;
import ija.game.model.unit.Unit;
import ija.game.model.unit.UnitType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class SimpleBotService {

    public record BotStep(boolean acted, boolean turnFinished, String message) {}

    private final Random random = new Random();

    public BotStep playOneStep(GameState state, GameEngine engine, boolean allowBuy) {
        if (state.isGameOver())
            return new BotStep(false, true, "Game is already over.");

        int playerId = state.getCurrentPlayerId();
        if (allowBuy) {
            String buyMessage = tryBuyUnit(state, engine, playerId);
            if (buyMessage != null)
                return new BotStep(true, false, buyMessage);
        }

        GameMap map = state.getMap();
        List<GameMap.UnitAtPosition> available = new ArrayList<>(map.getUnitsForPlayer(playerId).stream()
            .filter(u -> !u.unit().getHasActed())
            .toList());
        if (available.isEmpty())
            return new BotStep(false, true, "Bot finished its turn.");

        Collections.shuffle(available, random);
        Position from = available.getFirst().pos();
        Position current = moveTowardsEnemy(state, engine, from);

        if (tryAttack(state, engine, current))
            return new BotStep(true, false, "Bot attacked from " + current + ".");
        if (engine.waitUnit(current))
            return new BotStep(true, false, "Bot waited at " + current + ".");
        return new BotStep(false, false, "Bot could not act.");
    }

    private String tryBuyUnit(GameState state, GameEngine engine, int playerId) {
        List<Position> factories = findFreeOwnedFactories(state, playerId);
        if (factories.isEmpty())
            return null;

        List<UnitType> affordable = affordableTypes(state);
        if (affordable.isEmpty())
            return null;

        Position factory = factories.get(random.nextInt(factories.size()));
        UnitType type = affordable.get(random.nextInt(affordable.size()));
        var outcome = engine.buy(type, factory);
        return outcome.success() ? outcome.message() : null;
    }

    private List<Position> findFreeOwnedFactories(GameState state, int playerId) {
        List<Position> result = new ArrayList<>();
        GameMap map = state.getMap();

        for (int y = 0; y < map.getHeight(); y++) {
            for (int x = 0; x < map.getWidth(); x++) {
                var tile = map.getTile(x, y);
                var building = tile.getRawBuilding();
                if (building == null)
                    continue;
                if (!building.getType().allowsPurchase())
                    continue;
                if (building.getOwnerId() != playerId || tile.hasUnit())
                    continue;
                result.add(new Position(x, y));
            }
        }

        return result;
    }

    private List<UnitType> affordableTypes(GameState state) {
        List<UnitType> result = new ArrayList<>();
        int funds = state.getCurrentPlayer().getFunds();
        for (UnitType type : UnitType.values())
            if (type.getBuyCost() <= funds)
                result.add(type);
        return result;
    }

    private Position moveTowardsEnemy(GameState state, GameEngine engine, Position from) {
        Set<Position> reachable = engine.getReachableTiles(from);
        if (reachable.isEmpty())
            return from;

        List<Position> options = new ArrayList<>(reachable);
        options.sort(Comparator.comparingInt(p -> nearestEnemyDistance(state, p)));
        Position target = options.getFirst();

        if (target.equals(from))
            return from;
        return engine.tryMoveUnit(from, target) ? target : from;
    }

    private int nearestEnemyDistance(GameState state, Position pos) {
        int currentPlayer = state.getCurrentPlayerId();
        int best = Integer.MAX_VALUE;

        for (var other : state.getMap().getAllUnits()) {
            if (other.unit().getPlayerId() == currentPlayer)
                continue;
            int d = pos.manhattanDistance(other.pos());
            if (d < best)
                best = d;
        }

        return best;
    }

    private boolean tryAttack(GameState state, GameEngine engine, Position attackerPos) {
        GameMap map = state.getMap();
        Unit attacker = map.getTile(attackerPos).getUnit().orElse(null);
        if (attacker == null)
            return false;

        List<Position> targets = attackTargets(state, attacker, attackerPos);
        Collections.shuffle(targets, random);

        for (Position target : targets)
            if (engine.attack(attackerPos, target).success())
                return true;
        return false;
    }

    private List<Position> attackTargets(GameState state, Unit attacker, Position from) {
        List<Position> result = new ArrayList<>();
        int currentPlayer = state.getCurrentPlayerId();

        if (attacker.getType() == UnitType.ARTILLERY && attacker.getHasMoved())
            return result;

        for (var other : state.getMap().getAllUnits()) {
            if (other.unit().getPlayerId() == currentPlayer)
                continue;
            int distance = from.manhattanDistance(other.pos());
            if (attacker.getType().canAttackAt(distance))
                result.add(other.pos());
        }

        return result;
    }
}