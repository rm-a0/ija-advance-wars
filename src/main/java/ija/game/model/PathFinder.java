/**
 * Authors: Team xrepcim00 
 * Description: Dijkstra-based pathfinder for 
 * unit movement range and path calculation.
 */
package ija.game.model;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

public class PathFinder {

    private record State(Position pos, int cost) {}

    public static Set<Position> getReachableTiles(
        Unit unit, 
        Position from,
        GameMap map, 
        int currentPlayerId
    ) {
        int moveRange = unit.getType().getMoveRange();
        
        // Fill with max value
        int[][] bestCost = new int[map.getHeight()][map.getWidth()];
        for (int[] row : bestCost) 
            Arrays.fill(row, Integer.MAX_VALUE);

        // Track tiles that are passable but not stoppable (friendly unit standing there)
        Set<Position> passableOnly = new HashSet<>();

        // Dijkstra's algorithm using a priority queue
        PriorityQueue<State> queue = new PriorityQueue<>(Comparator.comparingInt(State::cost));
        bestCost[from.getY()][from.getX()] = 0;
        queue.offer(new State(from, 0));

        while (!queue.isEmpty()) {
            State current = queue.poll();
            int x = current.pos().getX();
            int y = current.pos().getY();

            if (current.cost() > bestCost[y][x]) 
                continue;

            for (Position neighbor : current.pos().neighbors()) {
                if (!map.isInBounds(neighbor)) 
                    continue;

                Tile tile = map.getTile(neighbor);

                // Enemy unit - impassable
                if (tile.hasUnit() && tile.getRawUnit().getPlayerId() != currentPlayerId)
                    continue;

                // Terrain impassable for this unit type
                int stepCost = tile.getTerrain().getMoveCost(unit.getType());
                if (stepCost >= 99) continue;

                int newCost = current.cost() + stepCost;
                if (newCost > moveRange) 
                    continue;
                if (newCost >= bestCost[neighbor.getY()][neighbor.getX()]) 
                    continue;

                bestCost[neighbor.getY()][neighbor.getX()] = newCost;

                // Friendly unit - passable but not stoppable 
                if (tile.hasUnit() && tile.getRawUnit().getPlayerId() == currentPlayerId)
                    passableOnly.add(neighbor);
                else
                    passableOnly.remove(neighbor);

                queue.offer(new State(neighbor, newCost));
            }
        }

        // All reachable positions except those blocked by friendly units
        Set<Position> reachable = new LinkedHashSet<>();
        reachable.add(from); // unit can always stay in place (0-cost move)

        for (int y = 0; y < map.getHeight(); y++) {
            for (int x = 0; x < map.getWidth(); x++) {
                if (bestCost[y][x] == Integer.MAX_VALUE) 
                    continue;
                Position p = new Position(x, y);
                if (p.equals(from)) 
                    continue;
                if (!passableOnly.contains(p)) 
                    reachable.add(p);
            }
        }

        return reachable;
    }

    public static List<Position> getPath(
        Unit unit, 
        Position from, 
        Position to,
        GameMap map, 
        int currentPlayerId
    ) {
        int moveRange = unit.getType().getMoveRange();

        int[][] bestCost = new int[map.getHeight()][map.getWidth()];
        for (int[] row : bestCost) Arrays.fill(row, Integer.MAX_VALUE);

        Map<Position, Position> parent = new HashMap<>();

        PriorityQueue<State> queue = new PriorityQueue<>(Comparator.comparingInt(State::cost));
        bestCost[from.getY()][from.getX()] = 0;
        queue.offer(new State(from, 0));

        while (!queue.isEmpty()) {
            State current = queue.poll();
            int x = current.pos().getX();
            int y = current.pos().getY();

            if (current.cost() > bestCost[y][x]) 
                continue;
            if (current.pos().equals(to)) 
                break; // found destination

            for (Position neighbor : current.pos().neighbors()) {
                if (!map.isInBounds(neighbor)) 
                    continue;

                Tile tile = map.getTile(neighbor);

                if (tile.hasUnit() && tile.getRawUnit().getPlayerId() != currentPlayerId)
                    continue;

                int stepCost = tile.getTerrain().getMoveCost(unit.getType());
                if (stepCost >= 99) 
                    continue;

                int newCost = current.cost() + stepCost;
                if (newCost > moveRange) 
                    continue;
                if (newCost >= bestCost[neighbor.getY()][neighbor.getX()]) 
                    continue;

                bestCost[neighbor.getY()][neighbor.getX()] = newCost;
                parent.put(neighbor, current.pos());
                queue.offer(new State(neighbor, newCost));
            }
        }

        // Reconstruct path by walking parent map from destination back to source
        if (bestCost[to.getY()][to.getX()] == Integer.MAX_VALUE)
            return List.of(); // unreachable

        LinkedList<Position> path = new LinkedList<>();
        Position current = to;
        while (!current.equals(from)) {
            path.addFirst(current);
            current = parent.get(current);
        }
        path.addFirst(from);
        return path;
    }
}