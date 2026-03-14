/**
 * Authors: Team xrepcim00
 * Description: Represents a single tile on the game map. Each tile has a 
 * terrain type, and may optionally contain a unit and/or a building.
 */
package ija.game.model;

import java.util.Optional;

public class Tile {

    private TerrainType terrain;
    private Unit unit;         // null if empty
    private Building building; // null if no building on this tile

    public Tile() {}

    public Tile(TerrainType terrain) {
        this.terrain = terrain;
    }

    public Tile(TerrainType terrain, Building building) {
        this.terrain  = terrain;
        this.building = building;
    }

    // Getters
    public boolean hasUnit()     { return unit != null; }
    public boolean hasBuilding() { return building != null; }

    // Safe getters (used in game)
    public Optional<Unit>     getUnit()     { return Optional.ofNullable(unit); }
    public Optional<Building> getBuilding() { return Optional.ofNullable(building); }

    // Raw getters (may return null)
    public Unit     getRawUnit()     { return unit; }
    public Building getRawBuilding() { return building; }
    public TerrainType getTerrain()  { return terrain; }

    // Setters
    public void setTerrain(TerrainType terrain) { this.terrain = terrain; }
    public void setUnit(Unit unit)              { this.unit = unit; }
    public void setBuilding(Building building)  { this.building = building; }
    public void setRawUnit(Unit unit)           { this.unit = unit; }
    public void setRawBuilding(Building b)      { this.building = b; }
}
