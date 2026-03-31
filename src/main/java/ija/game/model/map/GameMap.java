/**
 * Authors: Team xrepcim00 
 * Description: The game map containing tiles and 
 * units, with methods for access and queries.
 */
package ija.game.model.map;

import ija.game.model.unit.Unit;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class GameMap {

    public record UnitAtPosition(Unit unit, Position pos) {}

    private int width;
    private int height;
    private Tile[][] tiles;

    public GameMap() {}

    public GameMap(int width, int height) {
        this.width  = width;
        this.height = height;
        this.tiles  = new Tile[height][width];

        // y is row, x is column
        for (int y = 0; y < height; y++)
            for (int x = 0; x < width; x++)
                tiles[y][x] = new Tile(TerrainType.PLAIN);
    }

    public boolean isInBounds(Position p) {
        return p.getX() >= 0 && p.getX() < width
            && p.getY() >= 0 && p.getY() < height;
    }

    public boolean isInBounds(int x, int y) {
        return x >= 0 && x < width && y >= 0 && y < height;
    }

    public Tile getTile(Position p) {
        if (!isInBounds(p))
            throw new IllegalArgumentException("Position out of bounds: " + p);
        return tiles[p.getY()][p.getX()];
    }

    public Tile getTile(int x, int y) {
        if (!isInBounds(x, y))
            throw new IllegalArgumentException("Position out of bounds: (" + x + "," + y + ")");
        return tiles[y][x];
    }

    // Unit placement and movement
    public void placeUnit(Position p, Unit unit) {
        getTile(p).setUnit(unit);
    }

    public void removeUnit(Position p) {
        getTile(p).setUnit(null);
    }

    public void moveUnit(Position from, Position to) {
        Tile src = getTile(from);
        Unit unit = src.getRawUnit();
        if (unit == null)
            throw new IllegalStateException("No unit at " + from);
        src.setUnit(null);
        getTile(to).setUnit(unit);
    }

    // Queries
    public Optional<Position> findUnit(int unitId) {
        for (int y = 0; y < height; y++)
            for (int x = 0; x < width; x++)
                if (tiles[y][x].hasUnit()
                        && tiles[y][x].getRawUnit().getId() == unitId)
                    return Optional.of(new Position(x, y));
        return Optional.empty();
    }

    public List<UnitAtPosition> getAllUnits() {
        List<UnitAtPosition> result = new ArrayList<>();
        for (int y = 0; y < height; y++)
            for (int x = 0; x < width; x++)
                if (tiles[y][x].hasUnit())
                    result.add(new UnitAtPosition(tiles[y][x].getRawUnit(), new Position(x, y)));
        return result;
    }

    public List<UnitAtPosition> getUnitsForPlayer(int playerId) {
        return getAllUnits().stream()
                .filter(u -> u.unit().getPlayerId() == playerId)
                .toList();
    }

    public List<UnitAtPosition> getUnitsForPlayers(List<Integer> playerIds) {
        return getAllUnits().stream()
                .filter(u -> playerIds.contains(u.unit().getPlayerId()))
                .toList();
    }

    // Getters
    public int      getWidth()  { return width; }
    public int      getHeight() { return height; }
    public Tile[][] getTiles()  { return tiles; }

    // Setters
    public void setWidth(int width)      { this.width = width; }
    public void setHeight(int height)    { this.height = height; }
    public void setTiles(Tile[][] tiles) { this.tiles = tiles; }
}