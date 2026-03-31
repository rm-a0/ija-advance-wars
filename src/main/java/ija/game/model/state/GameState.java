/**
 * Authors: Team xrepcim00
 * Description: The overall game state containing the 
 * map, players, turn info, and win state.
 */
package ija.game.model.state;

import ija.game.model.map.GameMap;
import ija.game.model.player.Player;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Optional;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GameState {

    private GameMap map;
    private List<Player> players;
    private int currentPlayerId;
    private int turnNumber;
    private boolean gameOver;
    private int winnerId; // No winner = -1

    public GameState() { this.winnerId = -1; }

    public GameState(GameMap map, List<Player> players) {
        this.map             = map;
        this.players         = players;
        this.currentPlayerId = players.get(0).getId();
        this.turnNumber      = 1;
        this.gameOver        = false;
        this.winnerId        = -1;
    }

    // Player queries
    public Player getCurrentPlayer() {
        return getPlayer(currentPlayerId);
    }

    public Player getPlayer(int id) {
        return players.stream()
                .filter(p -> p.getId() == id)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No player with id: " + id));
    }

    public List<Player> getOpponents(int playerId) {
        return players.stream()
                .filter(p -> p.getId() != playerId)
                .toList();
    }

    @JsonIgnore
    public int getNextPlayerId() {
        int idx = players.indexOf(getPlayer(currentPlayerId));
        return players.get((idx + 1) % players.size()).getId();
    }

    // Win state
    @JsonIgnore
    public void setWinner(int playerId) {
        this.winnerId = playerId;
        this.gameOver = true;
    }

    public Optional<Integer> getWinner() {
        return gameOver ? Optional.of(winnerId) : Optional.empty();
    }

    // Getters
    public GameMap      getMap()             { return map; }
    public List<Player> getPlayers()         { return players; }
    public int          getCurrentPlayerId() { return currentPlayerId; }
    public int          getTurnNumber()      { return turnNumber; }
    public boolean      isGameOver()         { return gameOver; }
    public int          getWinnerId()        { return winnerId; }

    // Setters
    public void setMap(GameMap map)                     { this.map = map; }
    public void setPlayers(List<Player> players)        { this.players = players; }
    public void setCurrentPlayerId(int currentPlayerId) { this.currentPlayerId = currentPlayerId; }
    public void setTurnNumber(int turnNumber)           { this.turnNumber = turnNumber; }
    public void setGameOver(boolean gameOver)           { this.gameOver = gameOver; }
    public void setWinnerId(int winnerId)               { this.winnerId = winnerId; }
}