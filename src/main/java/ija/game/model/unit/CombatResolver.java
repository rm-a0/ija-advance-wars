/**
 * Authors: Team xrepcim00 
 * Description: Resolves combat between two units, applying 
 * damage and counter-attack logic.
 */
package ija.game.model.unit;

import ija.game.model.map.TerrainType;

public class CombatResolver {

    public record CombatResult(
        int damageToDefender,
        int damageToAttacker,
        boolean counterFired
    ) {}

    public static CombatResult resolve(
        Unit attacker, 
        Unit defender,
        TerrainType attackerTerrain, 
        TerrainType defenderTerrain,
        int distance
    ) {
        // Attack phase
        int baseDmg = attacker.getType().getBaseDamageAgainst(defender.getType());
        int dmgToDefender = calcDamage(baseDmg, attacker.getHp(), defenderTerrain.getDefenseBonus());
        defender.takeDamage(dmgToDefender);

        // Counter-attack phase
        boolean counterFired = false;
        int dmgToAttacker = 0;

        if (defender.isAlive() && defender.getType().canCounterAt(distance)) {
            int counterBase = defender.getType().getBaseDamageAgainst(attacker.getType());
            dmgToAttacker = calcDamage(counterBase, defender.getHp(), attackerTerrain.getDefenseBonus());
            attacker.takeDamage(dmgToAttacker);
            counterFired = true;
        }

        return new CombatResult(dmgToDefender, dmgToAttacker, counterFired);
    }

    // dmg = base * (hp / 100) * (1 - defenseBonus * 0.1) 
    private static int calcDamage(int baseDamage, int attackerHp, int defenseBonus) {
        double raw = baseDamage * (attackerHp / 100.0) * (1.0 - defenseBonus * 0.1);
        return (int) raw; // floor via cast
    }
}