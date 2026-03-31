package ija.game.engine;

import ija.game.model.Building;
import ija.game.model.BuildingType;
import ija.game.model.GameMap;
import ija.game.model.GameState;
import ija.game.model.Position;
import ija.game.model.Tile;
import ija.game.model.Unit;

public class CaptureService {

    public record CaptureResult(boolean attempted, boolean completed, boolean gameWon, String message) {
    }

    private static CaptureResult skipped(String message) {
        return new CaptureResult(false, false, false, message);
    }

    private static CaptureResult progress(String message) {
        return new CaptureResult(true, false, false, message);
    }

    private static CaptureResult done(boolean gameWon, String message) {
        return new CaptureResult(true, true, gameWon, message);
    }

    public CaptureResult capture(GameState state, Position pos) {
        GameMap map = state.getMap();
        if (!map.isInBounds(pos)) {
            return skipped("Out of bounds.");
        }

        Tile tile = map.getTile(pos);
        Unit unit = tile.getUnit().orElse(null);
        if (unit == null) {
            return skipped("No unit to capture with.");
        }
        if (unit.getPlayerId() != state.getCurrentPlayerId()) {
            return skipped("Not your unit.");
        }
        if (unit.getHasActed()) {
            return skipped("Unit already acted this turn.");
        }
        if (!unit.getType().canCapture()) {
            return skipped("Only infantry can capture.");
        }

        Building building = tile.getRawBuilding();
        if (building == null) {
            return skipped("No building on this tile.");
        }
        if (!building.getType().capturable()) {
            return skipped("This building cannot be captured.");
        }
        if (building.getOwnerId() == unit.getPlayerId()) {
            return skipped("Building already belongs to you.");
        }

        boolean captured = building.applyCapture(unit.getHp());
        unit.setHasActed(true);

        if (!captured) {
            return progress("Capture progress: " + building.getCapturePoints() + "/20");
        }

        int capturingPlayerId = unit.getPlayerId();
        BuildingType capturedType = building.getType();
        building.setOwner(capturingPlayerId);

        if (capturedType == BuildingType.HQ) {
            state.setWinner(capturingPlayerId);
            return done(true, "HQ captured. Player " + capturingPlayerId + " wins.");
        }
        return done(false, "Building captured.");
    }

    public void resetCaptureProgressIfLeavingBuilding(GameMap map, Position from) {
        if (!map.isInBounds(from)) {
            return;
        }
        Tile fromTile = map.getTile(from);
        Building building = fromTile.getRawBuilding();
        if (building == null) {
            return;
        }
        if (building.getCapturePoints() < 20) {
            building.resetCapture();
        }
    }
}
