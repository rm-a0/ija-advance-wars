/**
 * Authors: Team xrepcim00
 * Description: A position on the game map with x and y 
 * coordinates, plus utility methods for neighbors and distance.
 */
package ija.game.model;

import java.util.List;
import java.util.Objects;

public class Position {

    private int x;
    private int y;

    public Position() {}

    public Position(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public List<Position> neighbors() {
        return List.of(
            new Position(x, y - 1), // north
            new Position(x, y + 1), // south
            new Position(x - 1, y), // west
            new Position(x + 1, y)  // east
        );
    }

    public int manhattanDistance(Position other) {
        return Math.abs(this.x - other.x) + Math.abs(this.y - other.y);
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public void setX(int x) { this.x = x; }
    public void setY(int y) { this.y = y; }

    @Override
    public boolean equals(Object o) {
        if (this == o) 
            return true;
        if (!(o instanceof Position other)) 
            return false;
        return x == other.x && y == other.y;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ")";
    }
}