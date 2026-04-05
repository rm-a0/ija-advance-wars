/**
 * Authors: Team xrepcim00
 * Description: Creates unit instances for gameplay and map loading.
 */
package ija.game.model.unit;

public final class UnitFactory {

    private UnitFactory() {
    }

    public static Unit create(UnitType type, int playerId) {
        return create(type, playerId, type.getMaxHp());
    }

    public static Unit create(UnitType type, int playerId, int hp) {
        return new Unit(type, playerId, hp);
    }
}