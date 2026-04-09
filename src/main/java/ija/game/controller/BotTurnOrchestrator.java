/**
 * Authors: Team xrepcim00
 * Description: Handles bot autoplay loop timing, turn stepping, and related UI updates.
 */
package ija.game.controller;

import ija.game.engine.GameEngine;
import ija.game.engine.SimpleBotService;
import ija.game.io.GameLogService;
import ija.game.model.state.GameState;
import ija.game.view.GameView;
import javafx.animation.PauseTransition;
import javafx.util.Duration;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public class BotTurnOrchestrator {

    private static final Duration BOT_STEP_DELAY = Duration.millis(420);
    private static final int BOT_SAFETY_LIMIT = 200;

    private final GameView view;
    private final Supplier<GameState> stateSupplier;
    private final Supplier<GameEngine> engineSupplier;
    private final Supplier<GameLogService> logSupplier;
    private final BooleanSupplier replayModeSupplier;
    private final Runnable renderAction;
    private final SimpleBotService botService;

    private boolean enabled;
    private boolean running;
    private boolean buyPending;
    private int stepCount;

    public BotTurnOrchestrator(
        GameView view,
        Supplier<GameState> stateSupplier,
        Supplier<GameEngine> engineSupplier,
        Supplier<GameLogService> logSupplier,
        BooleanSupplier replayModeSupplier,
        Runnable renderAction
    ) {
        this.view = view;
        this.stateSupplier = stateSupplier;
        this.engineSupplier = engineSupplier;
        this.logSupplier = logSupplier;
        this.replayModeSupplier = replayModeSupplier;
        this.renderAction = renderAction;
        this.botService = new SimpleBotService();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean blockWhenRunning() {
        if (!running)
            return false;
        view.setStatus("Bot is playing...");
        return true;
    }

    public void toggle() {
        if (running) {
            enabled = false;
            running = false;
            view.setBotEnabled(false);
            view.setStatus("Bot mode disabled.");
            renderAction.run();
            return;
        }

        if (replayModeSupplier.getAsBoolean()) {
            view.setStatus("Replay mode is read-only.");
            return;
        }

        enabled = !enabled;
        view.setBotEnabled(enabled);
        view.setStatus(enabled ? "Bot mode enabled." : "Bot mode disabled.");
    }

    public void runIfNeeded() {
        if (running)
            return;

        GameState state = stateSupplier.get();
        if (!canRunTurn(state))
            return;

        running = true;
        buyPending = true;
        stepCount = 0;
        runStep();
    }

    private void runStep() {
        if (!running)
            return;

        GameState state = stateSupplier.get();
        if (!canRunTurn(state)) {
            running = false;
            renderAction.run();
            return;
        }

        GameEngine engine = engineSupplier.get();
        GameLogService logService = logSupplier.get();

        if (stepCount++ >= BOT_SAFETY_LIMIT) {
            running = false;
            view.setStatus("Bot auto-play paused (safety stop).");
            renderAction.run();
            return;
        }

        var step = botService.playOneStep(state, engine, buyPending);
        buyPending = false;

        if (step.acted())
            logService.record("BOT_STEP", state);

        if (step.turnFinished()) {
            logService.record("BOT_TURN", state);
            if (!state.isGameOver()) {
                engine.endTurn();
                logService.record("END_TURN", state);
            }
            buyPending = true;
        }

        if (state.isGameOver()) {
            running = false;
            view.setStatus("HQ captured. Player " + state.getWinnerId() + " wins.");
            renderAction.run();
            return;
        }

        view.setStatus(step.message());
        renderAction.run();

        if (!canRunTurn(state)) {
            running = false;
            return;
        }

        PauseTransition delay = new PauseTransition(BOT_STEP_DELAY);
        delay.setOnFinished(e -> runStep());
        delay.play();
    }

    private boolean canRunTurn(GameState state) {
        return enabled
            && !replayModeSupplier.getAsBoolean()
            && !state.isGameOver()
            && state.getCurrentPlayer().isBot();
    }
}
