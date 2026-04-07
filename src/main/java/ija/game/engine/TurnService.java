/**
 * Authors: Team xrepcim00
 * Description: Advances turns and resets unit action state for the active player.
 */
package ija.game.engine;

import ija.game.model.state.GameState;

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
