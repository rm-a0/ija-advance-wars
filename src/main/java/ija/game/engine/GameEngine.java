/**
 * Authors: Team xrepcim00
 * Description: Core game logic facade for movement, combat, purchase, capture, and turn flow.
 */
package ija.game.engine;

import ija.game.model.map.GameMap;
import ija.game.model.state.GameState;
import ija.game.model.map.PathFinder;
import ija.game.model.map.Position;
import ija.game.model.unit.Unit;
import ija.game.model.unit.UnitType;

import java.util.Set;

public class GameEngine {

    private final GameState state;
    private final TurnService turnService;
    private final CombatService combatService;
    private final EconomyService economyService;
    private final RepairService repairService;
    private final CaptureService captureService;
    private final PurchaseService purchaseService;

    public GameEngine(GameState state) {
        this.state = state;
        this.turnService = new TurnService();
        this.combatService = new CombatService();
        this.economyService = new EconomyService();
        this.repairService = new RepairService();
        this.captureService = new CaptureService();
        this.purchaseService = new PurchaseService();
    }

    public Set<Position> getReachableTiles(Position from) {
        GameMap map = state.getMap();
        if (!map.isInBounds(from))
            return Set.of();

        Unit unit = getControllableUnit(from);
        if (unit == null)
            return Set.of();

        return unit.getHasMoved()
            ? Set.of(from)
            : PathFinder.getReachableTiles(unit, from, map, state.getCurrentPlayerId());
    }

    public boolean tryMoveUnit(Position from, Position to) {
        GameMap map = state.getMap();
        if (!map.isInBounds(from) || !map.isInBounds(to))
            return false;

        Unit unit = getControllableUnit(from);
        if (unit == null || unit.getHasMoved() || map.getTile(to).hasUnit())
            return false;

        Set<Position> reachable = PathFinder.getReachableTiles(unit, from, map, state.getCurrentPlayerId());
        if (!reachable.contains(to))
            return false;

        captureService.resetCaptureProgressIfLeavingBuilding(map, from);
        map.moveUnit(from, to);
        unit.setHasMoved(true);
        return true;
    }

    public CombatService.AttackOutcome attack(Position attackerPos, Position defenderPos) {
        return combatService.attack(state, attackerPos, defenderPos);
    }

    public boolean waitUnit(Position pos) {
        Unit unit = getControllableUnit(pos);
        if (unit == null)
            return false;

        unit.setHasActed(true);
        return true;
    }

    public CaptureService.CaptureResult capture(Position pos) {
        return captureService.capture(state, pos);
    }

    public PurchaseService.PurchaseOutcome buy(UnitType type, Position factoryPos) {
        return purchaseService.buy(state, factoryPos, type);
    }

    public void endTurn() {
        turnService.endTurn(state);
        captureService.applyStartTurnCapture(state);
        if (state.isGameOver())
            return;
        economyService.applyStartTurnIncome(state);
        repairService.applyStartTurnRepairs(state);
    }

    private Unit getControllableUnit(Position pos) {
        GameMap map = state.getMap();
        if (!map.isInBounds(pos))
            return null;

        Unit unit = map.getTile(pos).getUnit().orElse(null);
        if (unit == null || unit.getPlayerId() != state.getCurrentPlayerId() || unit.getHasActed())
            return null;
        return unit;
    }
}
