package ija.game.controller;

import ija.game.engine.GameEngine;
import ija.game.model.map.GameMap;
import ija.game.model.state.GameState;
import ija.game.model.building.Building;
import ija.game.model.map.Position;
import ija.game.model.map.Tile;
import ija.game.model.unit.Unit;
import ija.game.model.unit.UnitType;
import ija.game.view.GameView;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

public class GameController {

    private final GameState state;
    private final GameView view;
    private final GameEngine engine;

    private Position selectedUnitPos;
    private Position focusedPos;
    private Position shopFactoryPos;
    private Set<Position> reachable;
    private Set<Position> attackTargets;

    public GameController(GameState state, GameView view) {
        this.state = state;
        this.view = view;
        this.engine = new GameEngine(state);

        this.view.setOnTileClicked(this::onTileClicked);
        this.view.setOnBuyInfantry(this::buyInfantry);
        this.view.setOnBuyTank(this::buyTank);
        this.view.setOnBuyArtillery(this::buyArtillery);
        renderSelection();
    }

    public void endTurn() {
        if (state.isGameOver()) {
            view.setStatus("Game over. Player " + state.getWinnerId() + " already won.");
            return;
        }
        engine.endTurn();
        if (state.isGameOver()) {
            clearSelection();
            shopFactoryPos = null;
            view.hideFactoryMenu();
            view.setStatus("HQ captured. Player " + state.getWinnerId() + " wins.");
            renderSelection();
            return;
        }
        clearSelection();
        shopFactoryPos = null;
        view.hideFactoryMenu();
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
        if (!map.isInBounds(clickedPos)) {
            shopFactoryPos = null;
            view.hideFactoryMenu();
            return;
        }
        boolean clickedSameAsPreviousFocus = clickedPos.equals(focusedPos);
        focusedPos = clickedPos;

        Tile clickedTile = map.getTile(clickedPos);
        Optional<Unit> clickedUnitOpt = clickedTile.getUnit();
        boolean clickedOwnActiveUnit = clickedUnitOpt.isPresent()
            && clickedUnitOpt.get().getPlayerId() == state.getCurrentPlayerId()
            && !clickedUnitOpt.get().getHasActed();

        if (clickedOwnActiveUnit) {
            shopFactoryPos = null;
            view.hideFactoryMenu();
        } else if (isOwnFactory(clickedTile) && !clickedSameAsPreviousFocus) {
            shopFactoryPos = clickedPos;
            view.showFactoryMenu();
        } else if (isOwnFactory(clickedTile) && view.isFactoryMenuVisible()) {
            shopFactoryPos = null;
            view.hideFactoryMenu();
        } else {
            shopFactoryPos = null;
            view.hideFactoryMenu();
        }

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
            attackTargets = collectAttackTargets(selectedUnitPos);
            view.setStatus("Moved to " + clickedPos + ". Attack enemy or click unit to wait.");
            renderSelection();
            return;
        }

        if (selectFriendlyUnit(clickedPos, clickedTile, false))
            return;

        view.setStatus("Invalid action.");
    }

    private void handleCaptureOrWait() {
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
        attackTargets = collectAttackTargets(selectedUnitPos);
        view.setStatus("Selected " + unit.getType() + " at " + selectedUnitPos);
        renderSelection();
        return true;
    }

    private void clearSelection() {
        selectedUnitPos = null;
        reachable = null;
        attackTargets = null;
    }

    private void renderSelection() {
        updateHud();
        view.render(state.getMap(), selectedUnitPos, reachable, attackTargets);
    }

    public void buyInfantry() {
        buy(UnitType.INFANTRY);
    }

    public void buyTank() {
        buy(UnitType.TANK);
    }

    public void buyArtillery() {
        buy(UnitType.ARTILLERY);
    }

    private void buy(UnitType type) {
        if (state.isGameOver()) {
            view.setStatus("Game over. Player " + state.getWinnerId() + " won.");
            shopFactoryPos = null;
            view.hideFactoryMenu();
            return;
        }

        var outcome = engine.buy(type, shopFactoryPos);
        view.setStatus(outcome.message() + " Funds: " + state.getCurrentPlayer().getFunds());
        if (outcome.success()) {
            shopFactoryPos = null;
            view.hideFactoryMenu();
        }
        renderSelection();
    }

    private boolean isOwnFactory(Tile tile) {
        Building building = tile.getRawBuilding();
        return building != null
            && building.getType().allowsPurchase()
            && building.getOwnerId() == state.getCurrentPlayerId();
    }

    private void updateHud() {
        view.setHud(state.getCurrentPlayerId(), state.getTurnNumber(), state.getCurrentPlayer().getFunds());
    }

    private Set<Position> collectAttackTargets(Position from) {
        GameMap map = state.getMap();
        if (!map.isInBounds(from))
            return Set.of();

        Unit attacker = map.getTile(from).getUnit().orElse(null);
        if (attacker == null)
            return Set.of();
        if (attacker.getPlayerId() != state.getCurrentPlayerId())
            return Set.of();
        if (attacker.getHasActed())
            return Set.of();
        if (attacker.getType() == UnitType.ARTILLERY && attacker.getHasMoved())
            return Set.of();

        Set<Position> result = new LinkedHashSet<>();
        for (var other : map.getAllUnits()) {
            if (other.unit().getPlayerId() == state.getCurrentPlayerId())
                continue;
            int distance = from.manhattanDistance(other.pos());
            if (attacker.getType().canAttackAt(distance))
                result.add(other.pos());
        }
        return result;
    }
}
