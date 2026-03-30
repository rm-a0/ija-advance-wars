package ija.game.controller;

import ija.game.model.GameMap;
import ija.game.model.GameState;
import ija.game.model.PathFinder;
import ija.game.model.Position;
import ija.game.model.Tile;
import ija.game.model.Unit;
import ija.game.view.GameView;

import java.util.Optional;
import java.util.Set;

public class GameController {

    private final GameState state;
    private final GameView view;

    private Position selectedUnitPos;
    private Set<Position> reachable;

    public GameController(GameState state, GameView view) {
        this.state = state;
        this.view = view;

        this.view.setOnTileClicked(this::onTileClicked);
        this.view.render(state.getMap(), selectedUnitPos, reachable);
    }

    private void onTileClicked(Position clickedPos) {
        GameMap map = state.getMap();
        if (!map.isInBounds(clickedPos)) {
            return;
        }

        Tile clickedTile = map.getTile(clickedPos);

        // If no unit selected, try selecting a friendly unit.
        if (selectedUnitPos == null) {
            Optional<Unit> unitOpt = clickedTile.getUnit();
            if (unitOpt.isEmpty()) {
                view.setStatus("No unit here.");
                return;
            }

            Unit unit = unitOpt.get();
            if (unit.getPlayerId() != state.getCurrentPlayerId()) {
                view.setStatus("Not your unit.");
                return;
            }

            selectedUnitPos = clickedPos;
            reachable = PathFinder.getReachableTiles(unit, selectedUnitPos, map, state.getCurrentPlayerId());
            view.setStatus("Selected " + unit.getType() + " at " + selectedUnitPos);
            view.render(map, selectedUnitPos, reachable);
            return;
        }

        // Unit selected: clicking the same tile cancels selection.
        if (clickedPos.equals(selectedUnitPos)) {
            selectedUnitPos = null;
            reachable = null;
            view.setStatus("Selection cleared.");
            view.render(map, null, null);
            return;
        }

        // If clicked tile is reachable and is not occupied by any unit, move.
        if (reachable != null && reachable.contains(clickedPos) && !clickedTile.hasUnit()) {
            map.moveUnit(selectedUnitPos, clickedPos);
            view.setStatus("Moved to " + clickedPos);
            selectedUnitPos = null;
            reachable = null;
            view.render(map, null, null);
            return;
        }

        // Otherwise try selecting a different friendly unit.
        Optional<Unit> unitOpt = clickedTile.getUnit();
        if (unitOpt.isPresent() && unitOpt.get().getPlayerId() == state.getCurrentPlayerId()) {
            selectedUnitPos = clickedPos;
            reachable = PathFinder.getReachableTiles(unitOpt.get(), selectedUnitPos, map, state.getCurrentPlayerId());
            view.setStatus("Selected " + unitOpt.get().getType() + " at " + selectedUnitPos);
            view.render(map, selectedUnitPos, reachable);
            return;
        }

        view.setStatus("Invalid move.");
    }
}
