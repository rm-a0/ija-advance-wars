/**
 * Authors: Team xrepcim00
 * Description: Loads terrain, unit, and damage stats 
 * from TSV files into enum registries at startup.
 */
package ija.game.io;

import ija.game.model.map.TerrainType;
import ija.game.model.unit.UnitType;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;

public class DataLoader {

    public static void loadAll() {
        loadAll("data");
    }

    public static void loadAll(String dataDir) {
        loadTerrainData(dataDir + "/terrain.tsv");
        loadUnitData(dataDir + "/units.tsv");
        loadDamageData(dataDir + "/units-damage.tsv");
    }

    public static void loadTerrainData(String filePath) {
        try (BufferedReader reader = Files.newBufferedReader(Path.of(filePath), StandardCharsets.UTF_8)) {
            loadTerrainDataFromReader(reader);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load terrain data: " + filePath, e);
        }
    }

    private static void loadTerrainDataFromReader(BufferedReader reader) throws IOException {
        reader.readLine(); // skip header
        String line;

        while ((line = reader.readLine()) != null) {
            if (line.isBlank())
                continue;

            String[] p     = line.split("\t");
            TerrainType t  = TerrainType.valueOf(p[0].trim());
            int defense    = Integer.parseInt(p[1].trim());
            int footCost   = Integer.parseInt(p[2].trim());
            int vehCost    = Integer.parseInt(p[3].trim());
            int income     = Integer.parseInt(p[4].trim());
            boolean heals  = Boolean.parseBoolean(p[5].trim());

            TerrainType.register(
                t,
                new TerrainType.Stats(
                    defense,
                    footCost,
                    vehCost,
                    income,
                    heals
                )
            );
        }
    }

    public static void loadUnitData(String filePath) {
        try (BufferedReader reader = Files.newBufferedReader(Path.of(filePath), StandardCharsets.UTF_8)) {
            loadUnitDataFromReader(reader);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load unit data: " + filePath, e);
        }
    }

    private static void loadUnitDataFromReader(BufferedReader reader) throws IOException {
        reader.readLine(); // skip header
        String line;

        while ((line = reader.readLine()) != null) {
            if (line.isBlank())
                continue;

            String[] p      = line.split("\t");
            UnitType t      = UnitType.valueOf(p[0].trim());
            int maxHp       = Integer.parseInt(p[1].trim());
            int moveRange   = Integer.parseInt(p[2].trim());
            boolean vehicle = p[3].trim().equals("VEHICLE");
            int minRange    = Integer.parseInt(p[4].trim());
            int maxRange    = Integer.parseInt(p[5].trim());
            int buyCost     = Integer.parseInt(p[6].trim());

            UnitType.register(
                t,
                new UnitType.Stats(
                    maxHp,
                    moveRange,
                    vehicle,
                    minRange,
                    maxRange,
                    buyCost
                )
            );
        }
    }

    public static void loadDamageData(String filePath) {
        try (BufferedReader reader = Files.newBufferedReader(Path.of(filePath), StandardCharsets.UTF_8)) {
            loadDamageDataFromReader(reader);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load damage data: " + filePath, e);
        }
    }

    private static void loadDamageDataFromReader(BufferedReader reader) throws IOException {
        // Read header to determine column order
        String header  = reader.readLine();
        String[] cols  = header.split("\t");

        // Map column indices to UnitType enums
        UnitType[] colTypes = new UnitType[cols.length - 1];
        for (int i = 1; i < cols.length; i++) {
            colTypes[i - 1] = UnitType.valueOf(cols[i].trim().replace("vs_", ""));
        }

        int size = UnitType.values().length;
        int[][] table = new int[size][size];

        String line;
        while ((line = reader.readLine()) != null) {
            if (line.isBlank())
                continue;

            String[] p        = line.split("\t");
            UnitType attacker = UnitType.valueOf(p[0].trim());

            // Fill damage values for this attacker against each defender
            for (int i = 1; i < p.length; i++) {
                UnitType defender = colTypes[i - 1];
                table[attacker.ordinal()][defender.ordinal()] = Integer.parseInt(p[i].trim());
            }
        }
        UnitType.setDamageTable(table);
    }
}
