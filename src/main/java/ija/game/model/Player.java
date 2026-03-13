/**
 * Authors: Team xrepcim00
 * Description: A player with funds and bot flag.
 */
package ija.game.model;

public class Player {

    private int id;
    private String name;
    private int funds;
    private boolean bot;

    public Player() {}

    public Player(int id, String name, int funds, boolean bot) {
        this.id    = id;
        this.name  = name;
        this.funds = funds;
        this.bot   = bot;
    }

    public void addFunds(int amount)  { funds += amount; }

    public boolean spendFunds(int amount) {
        if (funds < amount) return false;
        funds -= amount;
        return true;
    }

    public boolean canAfford(int amount) { return funds >= amount; }

    public int     getId()    { return id; }
    public String  getName()  { return name; }
    public int     getFunds() { return funds; }
    public boolean isBot()    { return bot; }

    public void setId(int id)         { this.id = id; }
    public void setName(String name)  { this.name = name; }
    public void setFunds(int funds)   { this.funds = funds; }
    public void setBot(boolean bot)   { this.bot = bot; }
}