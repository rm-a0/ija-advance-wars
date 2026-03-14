/**
 * Authors: Team xrepcim00
 * Description: Unit instance on the map with HP, 
 * ownership, and per-turn state flags.
 */
package ija.game.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.concurrent.atomic.AtomicInteger;

public class Unit {

    private static final AtomicInteger ID_COUNTER = new AtomicInteger(0);

    private int id;
    private UnitType type;
    private int hp;
    private int playerId;
    private boolean hasActed;
    private boolean hasMoved;

    public Unit() {}

    @JsonCreator
    public Unit(
        @JsonProperty("type")     UnitType type,
        @JsonProperty("playerId") int playerId,
        @JsonProperty("hp")       int hp
    ) {
        this.id       = ID_COUNTER.incrementAndGet();
        this.type     = type;
        this.playerId = playerId;
        this.hp       = hp;
    }

    @JsonIgnore
    public boolean isAlive() { return hp > 0; }

    public void takeDamage(int dmg) {
        hp = Math.max(0, hp - dmg);
    }

    public void heal(int amount) {
        hp = Math.min(type.getMaxHp(), hp + amount);
    }

    public void reset() {
        hasActed = false;
        hasMoved = false;
    }

    // Getters
    public int      getId()       { return id; }
    public UnitType getType()     { return type; }
    public int      getHp()       { return hp; }
    public int      getPlayerId() { return playerId; }
    @JsonProperty("hasActed")
    public boolean  getHasActed()    { return hasActed; }
    @JsonProperty("hasMoved")
    public boolean  getHasMoved()    { return hasMoved; }

    // Setters
    public void setId(int id) {
        this.id = id;
        // Ensure ID_COUNTER is always ahead of any manually set IDs
        ID_COUNTER.updateAndGet(c -> Math.max(c, id));
    }
    public void setType(UnitType type)         { this.type = type; }
    public void setHp(int hp)                  { this.hp = hp; }
    public void setPlayerId(int playerId)      { this.playerId = playerId; }
    public void setHasActed(boolean hasActed)  { this.hasActed = hasActed; }
    public void setHasMoved(boolean hasMoved)  { this.hasMoved = hasMoved; }
}
