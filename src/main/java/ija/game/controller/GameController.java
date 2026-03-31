package ija.game.controller;

import ija.game.engine.GameEngine;
import ija.game.model.map.GameMap;
import ija.game.model.state.GameState;
import ija.game.model.map.Position;
import ija.game.model.map.Tile;
import ija.game.model.unit.Unit;
import ija.game.view.GameView;

import java.util.Optional;
import java.util.Set;

public class GameController {

    private final GameState state;
    private final GameView view;
    private final GameEngine engine;

    private Position selectedUnitPos;
    private Set<Position> reachable;

    public GameController(GameState state, GameView view) {
        this.state = state;
        this.view = view;
        this.engine = new GameEngine(state);

        this.view.setOnTileClicked(this::onTileClicked);
        renderSelection();
    }

    public void endTurn() {
        if (state.isGameOver()) {
            view.setStatus("Game over. Player " + state.getWinnerId() + " already won.");
            return;
        }
        engine.endTurn();
        clearSelection();
        view.setStatus(
            "Turn " + state.getTurnNumber() +
            " - Player " + state.getCurrentPlayerId() +
            " (Funds: " + state.getCurrentPlayer().getFunds() + ")"
        );
        renderSelection();
    }

    private void onTileClicked(Position clickedPos) {
        if (state.isGameOver()) {
            view.setStatus("Game over. Player " + state.getWinnerId() + " won.");
            return;
        }

        GameMap map = state.getMap();
        if (!map.isInBounds(clickedPos))
            return;

        Tile clickedTile = map.getTile(clickedPos);

        if (selectedUnitPos == null) {
            selectFriendlyUnit(clickedPos, clickedTile, true);
            return;
        }

        if (clickedPos.equals(selectedUnitPos)) {
            handleCaptureOrWait();
            return;
        }

        Optional<Unit> targetUnitOpt = clickedTile.getUnit();
        if (targetUnitOpt.isPresent() && targetUnitOpt.get().getPlayerId() != state.getCurrentPlayerId()) {
            handleAttack(clickedPos);
            return;
        }

        if (engine.tryMoveUnit(selectedUnitPos, clickedPos)) {
            selectedUnitPos = clickedPos;
            reachable = engine.getReachableTiles(selectedUnitPos);
            view.setStatus("Moved to " + clickedPos + ". Attack enemy or click unit to wait.");
            renderSelection();
            return;
        }

        if (selectFriendlyUnit(clickedPos, clickedTile, false))
            return;

        view.setStatus("Invalid action.");
    }

    private void handleCaptureOrWait() {
        var captureResult = engine.capture(selectedUnitPos);
        if (captureResult.attempted()) {
            clearSelection();
            view.setStatus(captureResult.message());
            renderSelection();
            return;
        }

        if (engine.waitUnit(selectedUnitPos)) {
            clearSelection();
            view.setStatus("Unit waits.");
            renderSelection();
            return;
        }

        clearSelection();
        view.setStatus("Selection cleared.");
        renderSelection();
    }

    private void handleAttack(Position targetPos) {
        GameEngine.AttackOutcome outcome = engine.attack(selectedUnitPos, targetPos);
        if (!outcome.success()) {
            view.setStatus(outcome.message());
            return;
        }

        clearSelection();
        view.setStatus(
            "Attack dealt " + outcome.result().damageToDefender() +
            ", counter dealt " + outcome.result().damageToAttacker() + "."
        );
        renderSelection();
    }

    private boolean selectFriendlyUnit(Position pos, Tile tile, boolean showEmptyMessage) {
        Optional<Unit> unitOpt = tile.getUnit();
        if (unitOpt.isEmpty()) {
            if (showEmptyMessage)
                view.setStatus("No unit here.");
            return false;
        }

        Unit unit = unitOpt.get();
        if (unit.getPlayerId() != state.getCurrentPlayerId()) {
            view.setStatus("Not your unit.");
            return false;
        }
        if (unit.getHasActed()) {
            view.setStatus("Unit already acted this turn.");
            return false;
        }

        selectedUnitPos = pos;
        reachable = engine.getReachableTiles(selectedUnitPos);
        view.setStatus("Selected " + unit.getType() + " at " + selectedUnitPos);
        renderSelection();
        return true;
    }

    private void clearSelection() {
        selectedUnitPos = null;
        reachable = null;
    }

    private void renderSelection() {
        view.render(state.getMap(), selectedUnitPos, reachable);
    }
}
