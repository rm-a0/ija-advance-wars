/**
 * Authors: Team xrepcim00
 * Description: Coordinates game state, player input handling, replay controls, and UI updates.
 */
package ija.game.controller;

import ija.game.engine.GameEngine;
import ija.game.io.GameLogService;
import ija.game.io.GameReplayService;
import ija.game.io.MapLoader;
import ija.game.model.map.GameMap;
import ija.game.model.state.GameState;
import ija.game.model.building.Building;
import ija.game.model.map.Position;
import ija.game.model.map.Tile;
import ija.game.model.unit.Unit;
import ija.game.model.unit.UnitType;
import ija.game.view.GameView;

import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

public class GameController {

    private static final Path MANUAL_SAVE_PATH = Path.of("data/saves/manual_save.json");
    private static final Path LOG_ROOT = Path.of("data/logs");

    private GameState state;
    private final GameView view;
    private GameEngine engine;
    private GameLogService logService;
    private GameReplayService replayService;
    private final BotTurnOrchestrator botTurns;
    private boolean replayMode;

    private Position selectedUnitPos;
    private Position focusedPos;
    private Position shopFactoryPos;
    private Set<Position> reachable;
    private Set<Position> attackTargets;

    public GameController(GameState state, GameView view) {
        this(state, view, GameLogService.disabled());
    }

    public GameController(GameState state, GameView view, GameLogService logService) {
        this.state = state;
        this.view = view;
        this.logService = logService;
        this.engine = new GameEngine(state);
        this.botTurns = new BotTurnOrchestrator(
            view,
            () -> this.state,
            () -> this.engine,
            () -> this.logService,
            () -> this.replayMode,
            this::renderSelection
        );

        this.view.setOnTileClicked(this::onTileClicked);
        this.view.setOnBuyInfantry(this::buyInfantry);
        this.view.setOnBuyTank(this::buyTank);
        this.view.setOnBuyArtillery(this::buyArtillery);
        this.view.setOnSaveGame(this::saveGame);
        this.view.setOnLoadGame(this::loadSavedGame);
        this.view.setOnReplayLoad(this::loadReplay);
        this.view.setOnReplayPrev(this::replayPrev);
        this.view.setOnReplayNext(this::replayNext);
        this.view.setOnReplayLive(this::resumeLive);
        this.view.setOnToggleBot(this::toggleBot);
        this.view.setBotEnabled(botTurns.isEnabled());
        renderSelection();
    }

    public void endTurn() {
        if (blockWhenBotRunning() || blockWhenReplay("Use Replay controls: Previous, Next, Live."))
            return;
        if (blockWhenGameOver("Game over. Player " + state.getWinnerId() + " already won."))
            return;

        engine.endTurn();
        logService.record("END_TURN", state);

        if (state.isGameOver()) {
            resetTransientUi();
            view.setStatus("HQ captured. Player " + state.getWinnerId() + " wins.");
            renderSelection();
            return;
        }

        resetTransientUi();
        view.setStatus(
            "Turn " + state.getTurnNumber() +
            " - Player " + state.getCurrentPlayerId() +
            " (Funds: " + state.getCurrentPlayer().getFunds() + ")"
        );
        runBotTurnsIfNeeded();
        renderSelection();
    }

    private void onTileClicked(Position clickedPos) {
        if (blockWhenBotRunning() || blockWhenReplay("Use Replay controls: Previous, Next, Live."))
            return;
        if (blockWhenGameOver("Game over. Player " + state.getWinnerId() + " won."))
            return;

        GameMap map = state.getMap();
        if (!map.isInBounds(clickedPos)) {
            clearFactoryMenu();
            return;
        }
        boolean clickedSameAsPreviousFocus = clickedPos.equals(focusedPos);
        focusedPos = clickedPos;

        Tile clickedTile = map.getTile(clickedPos);
        Optional<Unit> clickedUnitOpt = clickedTile.getUnit();
        updateFactoryMenuState(clickedPos, clickedTile, clickedSameAsPreviousFocus, clickedUnitOpt);

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
            logService.record("MOVE", state);
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
            logService.record("WAIT", state);
            renderSelection();
            return;
        }

        clearSelection();
        view.setStatus("Selection cleared.");
        renderSelection();
    }

    private void handleAttack(Position targetPos) {
        var outcome = engine.attack(selectedUnitPos, targetPos);
        if (!outcome.success()) {
            view.setStatus(outcome.message());
            return;
        }

        clearSelection();
        view.setStatus(
            "Attack dealt " + outcome.result().damageToDefender() +
            ", counter dealt " + outcome.result().damageToAttacker() + "."
        );
        logService.record("ATTACK", state);
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
        updateSessionBanner();
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
        if (blockWhenBotRunning() || blockWhenReplay("Replay mode is read-only."))
            return;
        if (blockWhenGameOver("Game over. Player " + state.getWinnerId() + " won.")) {
            clearFactoryMenu();
            return;
        }

        var outcome = engine.buy(type, shopFactoryPos);
        view.setStatus(outcome.message() + " Funds: " + state.getCurrentPlayer().getFunds());
        if (outcome.success()) {
            clearFactoryMenu();
            logService.record("BUY", state);
        }
        renderSelection();
    }

    public void saveGame() {
        if (blockWhenBotRunning())
            return;
        if (blockWhenReplay("Return to Live mode before saving."))
            return;

        MapLoader.saveGame(state, MANUAL_SAVE_PATH.toString());
        logService.record("SAVE", state);
        view.setStatus("Saved game to " + MANUAL_SAVE_PATH + ".");
    }

    public void loadSavedGame() {
        if (blockWhenBotRunning())
            return;

        Optional<GameState> loadedState = MapLoader.tryLoadGame(MANUAL_SAVE_PATH.toString());
        if (loadedState.isEmpty()) {
            view.setStatus("Load failed.");
            return;
        }
        state = loadedState.get();

        replayService = null;
        replayMode = false;
        engine = new GameEngine(state);
        logService = GameLogService.startSession(LOG_ROOT, state);
        resetTransientUi();
        view.setStatus("Loaded save from " + MANUAL_SAVE_PATH + ".");
        runBotTurnsIfNeeded();
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

    private void loadReplay() {
        if (blockWhenBotRunning())
            return;
        if (replayMode) {
            view.setStatus("Replay already active.");
            return;
        }

        replayService = GameReplayService.loadLatest(LOG_ROOT).orElse(null);
        if (replayService == null) {
            view.setStatus("No replay log found.");
            return;
        }

        enterReplayState(replayService.currentState(), replayService.currentLabel());
    }

    private void replayPrev() {
        if (!ensureReplayLoaded("Load a replay first."))
            return;
        enterReplayState(replayService.previous(), replayService.currentLabel());
    }

    private void replayNext() {
        if (!ensureReplayLoaded("Load a replay first."))
            return;
        enterReplayState(replayService.next(), replayService.currentLabel());
    }

    private void resumeLive() {
        if (blockWhenBotRunning())
            return;
        if (!ensureReplayLoaded("Replay is not active."))
            return;

        replayService.deleteSession();
        replayService = null;
        replayMode = false;
        logService = GameLogService.startSession(LOG_ROOT, state);
        engine = new GameEngine(state);
        resetTransientUi();
        view.setStatus("Returned to live game.");
        runBotTurnsIfNeeded();
        renderSelection();
    }

    private void enterReplayState(GameState newState, String label) {
        state = newState;
        engine = new GameEngine(state);
        replayMode = true;
        resetTransientUi();
        view.setStatus("Replay: " + label);
        renderSelection();
    }

    private void updateSessionBanner() {
        view.setSessionMode(replayMode);
        view.setBotEnabled(botTurns.isEnabled());
    }

    private void toggleBot() {
        botTurns.toggle();
        runBotTurnsIfNeeded();
        renderSelection();
    }

    private boolean blockWhenGameOver(String message) {
        if (!state.isGameOver())
            return false;
        view.setStatus(message);
        return true;
    }

    private boolean blockWhenReplay(String message) {
        if (!replayMode)
            return false;
        view.setStatus(message);
        return true;
    }

    private boolean ensureReplayLoaded(String message) {
        if (replayMode && replayService != null)
            return true;
        view.setStatus(message);
        return false;
    }

    private void resetTransientUi() {
        clearSelection();
        clearFactoryMenu();
    }

    private void runBotTurnsIfNeeded() {
        botTurns.runIfNeeded();
    }

    private boolean blockWhenBotRunning() {
        return botTurns.blockWhenRunning();
    }

    private void updateFactoryMenuState(Position clickedPos, Tile clickedTile, boolean clickedSameAsPreviousFocus, Optional<Unit> clickedUnitOpt) {
        boolean clickedOwnActiveUnit = clickedUnitOpt.isPresent()
            && clickedUnitOpt.get().getPlayerId() == state.getCurrentPlayerId()
            && !clickedUnitOpt.get().getHasActed();

        if (clickedOwnActiveUnit) {
            clearFactoryMenu();
            return;
        }
        if (!isOwnFactory(clickedTile)) {
            clearFactoryMenu();
            return;
        }
        if (!clickedSameAsPreviousFocus) {
            shopFactoryPos = clickedPos;
            view.showFactoryMenu();
            return;
        }

        if (view.isFactoryMenuVisible()) {
            clearFactoryMenu();
            return;
        }

        shopFactoryPos = clickedPos;
        view.showFactoryMenu();
    }

    private void clearFactoryMenu() {
        shopFactoryPos = null;
        view.hideFactoryMenu();
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
