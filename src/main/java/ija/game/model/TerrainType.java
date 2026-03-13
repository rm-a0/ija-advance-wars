/**
 * Authors: Team xrepcim00
 * Description: Terrain types with stats loaded from 
 * terrain.tsv at startup.
 */
package ija.game.model;

import java.util.EnumMap;
import java.util.Map;

public enum TerrainType {
    PLAIN, FOREST, MOUNTAIN, CITY, WATER, FACTORY, HQ;

    public record Stats(
        int defenseBonus, 
        int footMoveCost, 
        int vehicleMoveCost,
        int income, 
        boolean heals
    ) {}

    private static final Map<TerrainType, Stats> STATS = new EnumMap<>(TerrainType.class);

    public static void register(TerrainType type, Stats stats) {
        STATS.put(type, stats);
    }

    public Stats getStats() {
        Stats s = STATS.get(this);
        if (s == null) 
            throw new IllegalStateException("Stats not loaded for terrain: " + this);
        return s;
    }

    public int getMoveCost(UnitType unitType) {
        Stats s = getStats();
        return unitType.isVehicle() ? s.vehicleMoveCost() : s.footMoveCost();
    }

    public boolean isPassableFor(UnitType unitType) {
        return getMoveCost(unitType) < 99;
    }

    public int     getDefenseBonus() { return getStats().defenseBonus(); }
    public int     getIncome()       { return getStats().income(); }
    public boolean heals()           { return getStats().heals(); }
}
