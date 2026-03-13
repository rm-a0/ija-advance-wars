/**
 * Authors: Team xrepcim00
 * Description: Unit types with stats and damage table 
 * loaded from TSV files at startup.
 */
package ija.game.model;

import java.util.EnumMap;
import java.util.Map;

public enum UnitType {
    INFANTRY, TANK, ARTILLERY;

    public record Stats(
        int maxHp, 
        int moveRange, 
        boolean vehicle,
        int minAttackRange, 
        int maxAttackRange, 
        int buyCost
    ) {}

    private static final Map<UnitType, Stats> STATS = new EnumMap<>(UnitType.class);
    private static int[][] damageTable;

    public static void register(UnitType type, Stats stats) {
        STATS.put(type, stats);
    }

    public static void setDamageTable(int[][] table) {
        damageTable = table;
    }

    public Stats getStats() {
        Stats s = STATS.get(this);
        if (s == null) 
            throw new IllegalStateException("Stats not loaded for unit type: " + this);
        return s;
    }

    public boolean isVehicle()  { return getStats().vehicle(); }
    public boolean canCapture() { return this == INFANTRY; }
    public int getMaxHp()       { return getStats().maxHp(); }
    public int getMoveRange()   { return getStats().moveRange(); }
    public int getMinRange()    { return getStats().minAttackRange(); }
    public int getMaxRange()    { return getStats().maxAttackRange(); }
    public int getBuyCost()     { return getStats().buyCost(); }

    public boolean canAttackAt(int distance) {
        Stats s = getStats();
        return distance >= s.minAttackRange() && distance <= s.maxAttackRange();
    }

    public boolean canCounterAt(int distance) {
        return canAttackAt(distance);
    }

    public int getBaseDamageAgainst(UnitType defender) {
        if (damageTable == null)
            throw new IllegalStateException("Damage table not loaded");
        return damageTable[this.ordinal()][defender.ordinal()];
    }
}