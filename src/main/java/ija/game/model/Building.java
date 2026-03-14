/**
 * Authors: Team xrepcim00
 * Description: Represents a building on the game map. 
 * Buildings can be captured by infantry and provide various benefits.
 */
package ija.game.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Building {

    private static final int MAX_CAPTURE_POINTS = 20;

    private BuildingType type;
    private int ownerId; // -1 = neutral
    private int capturePoints;

    public Building() {}

    public Building(BuildingType type, int ownerId) {
        this.type          = type;
        this.ownerId       = ownerId;
        this.capturePoints = MAX_CAPTURE_POINTS;
    }

    public boolean applyCapture(int infantryHp) {
        capturePoints -= infantryHp / 10;
        return capturePoints <= 0;
    }

    public void resetCapture() {
        capturePoints = MAX_CAPTURE_POINTS;
    }

    public void setOwner(int playerId) {
        this.ownerId = playerId;
        resetCapture();
    }

    // Getters
    @JsonIgnore
    public boolean isNeutral()             { return ownerId == -1; }
    @JsonIgnore
    public boolean isOwnedBy(int playerId) { return ownerId == playerId; }
    public BuildingType getType()          { return type; }
    public int          getOwnerId()       { return ownerId; }
    public int          getCapturePoints() { return capturePoints; }

    // Setters
    public void setType(BuildingType type)          { this.type = type; }
    public void setOwnerId(int ownerId)             { this.ownerId = ownerId; }
    public void setCapturePoints(int capturePoints) { this.capturePoints = capturePoints; }
}
