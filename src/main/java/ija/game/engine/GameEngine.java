package ija.game.engine;

import ija.game.model.CombatResolver;
import ija.game.model.GameMap;
import ija.game.model.GameState;
import ija.game.model.PathFinder;
import ija.game.model.Position;
import ija.game.model.Tile;
import ija.game.model.Unit;

import java.util.Set;

public class GameEngine {

    private final GameState state;
    private final TurnService turnService;
    private final CombatService combatService;
    private final EconomyService economyService;
    private final RepairService repairService;
    private final CaptureService captureService;

    public GameEngine(GameState state) {
        this.state = state;
        this.turnService = new TurnService();
        this.combatService = new CombatService();
        this.economyService = new EconomyService();
        this.repairService = new RepairService();
        this.captureService = new CaptureService();
    }

    public Set<Position> getReachableTiles(Position from) {
        GameMap map = state.getMap();
        if (!map.isInBounds(from)) {
            return Set.of();
        }

        Unit unit = getControllableUnit(from);
        if (unit == null) {
            return Set.of();
        }
        if (unit.getHasMoved()) {
            return Set.of(from);
        }

        return PathFinder.getReachableTiles(unit, from, map, state.getCurrentPlayerId());
    }

    public boolean tryMoveUnit(Position from, Position to) {
        GameMap map = state.getMap();
        if (!map.isInBounds(from) || !map.isInBounds(to)) {
            return false;
        }

        Tile dstTile = map.getTile(to);

        Unit unit = getControllableUnit(from);
        if (unit == null) {
            return false;
        }
        if (unit.getHasMoved()) {
            return false;
        }
        if (dstTile.hasUnit()) {
            return false;
        }

        Set<Position> reachable = PathFinder.getReachableTiles(unit, from, map, state.getCurrentPlayerId());
        if (!reachable.contains(to)) {
            return false;
        }

        captureService.resetCaptureProgressIfLeavingBuilding(map, from);
        map.moveUnit(from, to);
        unit.setHasMoved(true);
        return true;
    }

    public CombatResolver.CombatResult attack(Position attackerPos, Position defenderPos) {
        return combatService.attack(state, attackerPos, defenderPos);
    }

    public boolean waitUnit(Position pos) {
        Unit unit = getControllableUnit(pos);
        if (unit == null) {
            return false;
        }

        unit.setHasActed(true);
        return true;
    }

    public CaptureService.CaptureResult capture(Position pos) {
        return captureService.capture(state, pos);
    }

    public void endTurn() {
        turnService.endTurn(state);
        economyService.applyStartTurnIncome(state);
        repairService.applyStartTurnRepairs(state);
    }

    private Unit getControllableUnit(Position pos) {
        GameMap map = state.getMap();
        if (!map.isInBounds(pos)) {
            return null;
        }

        Unit unit = map.getTile(pos).getUnit().orElse(null);
        if (unit == null) {
            return null;
        }
        if (unit.getPlayerId() != state.getCurrentPlayerId() || unit.getHasActed()) {
            return null;
        }
        return unit;
    }
}
