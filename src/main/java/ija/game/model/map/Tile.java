/**
 * Authors: Team xrepcim00
 * Description: Represents a single tile on the game map. Each tile has a 
 * terrain type, and may optionally contain a unit and/or a building.
 */
package ija.game.model.map;

import ija.game.model.building.Building;
import ija.game.model.unit.Unit;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

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
    @JsonIgnore
    public Optional<Unit>     getUnit()     { return Optional.ofNullable(unit); }
    @JsonIgnore
    public Optional<Building> getBuilding() { return Optional.ofNullable(building); }

    // Raw getters (may return null)
    @JsonProperty("unit")
    public Unit     getRawUnit()     { return unit; }
    @JsonProperty("building")
    public Building getRawBuilding() { return building; }
    @JsonProperty("terrain")
    public TerrainType getTerrain()  { return terrain; }

    // Setters
    public void setTerrain(TerrainType terrain) { this.terrain = terrain; }
    public void setUnit(Unit unit)              { this.unit = unit; }
    public void setBuilding(Building building)  { this.building = building; }
    public void setRawUnit(Unit unit)           { this.unit = unit; }
    public void setRawBuilding(Building b)      { this.building = b; }
}
