/**
 * Authors: Team xrepcim00
 * Description: Building types with hardcoded flags 
 * for income, healing, purchasing, and capturability.
 */
package ija.game.model;

public enum BuildingType {
    CITY    (true,  true,   false,  true),
    FACTORY (true,  true,   true,   true),
    HQ      (true,  true,   false,  true);

    private final boolean generatesIncome;
    private final boolean heals;
    private final boolean allowsPurchase;
    private final boolean capturable;

    BuildingType(
        boolean generatesIncome, 
        boolean heals,
        boolean allowsPurchase, 
        boolean capturable
    ) {
        this.generatesIncome = generatesIncome;
        this.heals           = heals;
        this.allowsPurchase  = allowsPurchase;
        this.capturable      = capturable;
    }

    public boolean generatesIncome() { return generatesIncome; }
    public boolean heals()           { return heals; }
    public boolean allowsPurchase()  { return allowsPurchase; }
    public boolean capturable()      { return capturable; }
}