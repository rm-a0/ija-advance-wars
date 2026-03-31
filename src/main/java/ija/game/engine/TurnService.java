package ija.game.engine;

import ija.game.model.GameMap;
import ija.game.model.GameState;

public class TurnService {

    public void endTurn(GameState state) {
        int nextPlayerId = state.getNextPlayerId();
        state.setCurrentPlayerId(nextPlayerId);
        state.setTurnNumber(state.getTurnNumber() + 1);
        resetUnitsForCurrentPlayer(state);
    }

    private void resetUnitsForCurrentPlayer(GameState state) {
        GameMap map = state.getMap();
        map.getUnitsForPlayer(state.getCurrentPlayerId())
            .forEach(u -> u.unit().reset());
    }
}
