package ija.game.engine;

import ija.game.model.GameState;

public class TurnService {

    public void endTurn(GameState state) {
        int nextPlayerId = state.getNextPlayerId();
        state.setCurrentPlayerId(nextPlayerId);
        state.setTurnNumber(state.getTurnNumber() + 1);
        resetUnitsForCurrentPlayer(state);
    }

    private void resetUnitsForCurrentPlayer(GameState state) {
        state.getMap().getUnitsForPlayer(state.getCurrentPlayerId())
            .forEach(u -> u.unit().reset());
    }
}
