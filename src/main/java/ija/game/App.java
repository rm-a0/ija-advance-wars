/**
 * Authors: Team xrepcim00
 * Description: Application entry point. 
 * Bootstraps data loading and launches the game.
 */
package ija.game;

import ija.game.io.DataLoader;
import ija.game.model.*;

public class App {

    public static void main(String[] args) {
        // Load game data from TSV
        DataLoader.loadAll("data");
        System.out.println("Data loaded");

        // Verify TerrainType stats
        System.out.println("\nTerrain");
        for (TerrainType t : TerrainType.values()) {
            System.out.printf("%-10s defense=%-2d foot=%-3d vehicle=%-3d income=%-5d heals=%s%n",
                t,
                t.getDefenseBonus(),
                t.getStats().footMoveCost(),
                t.getStats().vehicleMoveCost(),
                t.getIncome(),
                t.heals()
            );
        }

        // Verify UnitType stats
        System.out.println("\nUnits");
        for (UnitType u : UnitType.values()) {
            System.out.printf("%-12s maxHp=%-4d move=%-3d vehicle=%-6s range=%d-%d cost=%d%n",
                u,
                u.getMaxHp(),
                u.getMoveRange(),
                u.isVehicle(),
                u.getMinRange(),
                u.getMaxRange(),
                u.getBuyCost()
            );
        }

        // Verify damage table
        System.out.println("\nDamage Table");
        System.out.printf("%-12s vs_INF  vs_TANK  vs_ART%n", "attacker");
        for (UnitType attacker : UnitType.values()) {
            System.out.printf("%-12s %-7d %-8d %-7d%n",
                attacker,
                attacker.getBaseDamageAgainst(UnitType.INFANTRY),
                attacker.getBaseDamageAgainst(UnitType.TANK),
                attacker.getBaseDamageAgainst(UnitType.ARTILLERY)
            );
        }
    }
}