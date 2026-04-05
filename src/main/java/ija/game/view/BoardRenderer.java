/**
 * Authors: Team xrepcim00
 * Description: Renders map tiles, unit markers, overlays, and capture progress indicators.
 */
package ija.game.view;

import ija.game.model.map.GameMap;
import ija.game.model.map.Position;
import ija.game.model.map.TerrainType;
import ija.game.model.map.Tile;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineJoin;

import java.util.Set;

public class BoardRenderer {

    private static final double SPRITE_W = 128.0;
    private static final double SPRITE_H = 128.0;

    private final SpriteStore sprites;

    public BoardRenderer(SpriteStore sprites) {
        this.sprites = sprites;
    }

    // Main rendering method to draw the game board with all tiles, units, and overlays
    public void draw(
        GraphicsContext g,
        GameMap map,
        Position selectedUnitPos,
        Set<Position> reachable,
        Set<Position> attackTargets,
        Position focusedTilePos,
        Position hoveredTilePos,
        IsometricCamera camera,
        double canvasWidth,
        double canvasHeight,
        double originX,
        double originY
    ) {
        g.setImageSmoothing(true);
        g.setFill(Color.rgb(18, 18, 20));
        g.fillRect(0, 0, canvasWidth, canvasHeight);

        double minWX = (-camera.getCamX()) / camera.getZoom() - SPRITE_W;
        double minWY = (-camera.getCamY()) / camera.getZoom() - SPRITE_H;
        double maxWX = (canvasWidth - camera.getCamX()) / camera.getZoom() + SPRITE_W;
        double maxWY = (canvasHeight - camera.getCamY()) / camera.getZoom() + SPRITE_H;

        g.save();
        g.translate(camera.getCamX(), camera.getCamY());
        g.scale(camera.getZoom(), camera.getZoom());

        int width = map.getWidth();
        int height = map.getHeight();

        // Iterate over tiles in diagonal order to ensure correct rendering of overlapping sprites
        for (int sum = 0; sum <= (width - 1) + (height - 1); sum++) {
            for (int y = 0; y < height; y++) {
                int x = sum - y;
                if (x < 0 || x >= width)
                    continue;
                
                // Calculate screen position of the tile's top point
                double[] top = IsoGeometry.tileTop(originX, originY, x, y);
                double tx = top[0];
                double ty = top[1];

                double bbMinX = tx - SPRITE_W * 0.5;
                double bbMaxX = bbMinX + SPRITE_W;
                double bbMinY = ty + IsoGeometry.TILE_H - SPRITE_H;
                double bbMaxY = bbMinY + SPRITE_H;
                if (bbMaxX < minWX || bbMinX > maxWX || bbMaxY < minWY || bbMinY > maxWY)
                    continue;
                
                // Render the tile's terrain sprite or fallback color
                Tile tile = map.getTile(x, y);
                Image terrainImg = resolveTerrainImage(tile);
                if (terrainImg != null) {
                    g.drawImage(
                        terrainImg,
                        tx - SPRITE_W * 0.5,
                        ty + IsoGeometry.TILE_H - SPRITE_H,
                        SPRITE_W,
                        SPRITE_H
                    );
                } else {
                    g.setFill(terrainColor(tile.getTerrain()));
                    IsoGeometry.fillDiamond(g, tx, ty, IsoGeometry.TILE_W, IsoGeometry.TILE_H);
                }

                Position p = new Position(x, y);
                
                // Highlight focused tile with a semi-transparent fill overlay
                if (focusedTilePos != null && focusedTilePos.equals(p)) {
                    drawFillOverlay(
                        g, 
                        tx, 
                        ty + 0.4, 
                        IsoGeometry.TILE_W - 2.0, 
                        IsoGeometry.TILE_H - 2.0, 
                        Color.rgb(255, 210, 80, 0.20)
                    );
                }
                
                // Highlight hovered tile with a bright stroke overlay
                if (hoveredTilePos != null && hoveredTilePos.equals(p)) {
                    drawStrokeOverlay(
                        g, 
                        tx, 
                        ty + 0.2, 
                        IsoGeometry.TILE_W - 1.0, 
                        IsoGeometry.TILE_H - 1.0, 
                        Color.rgb(255, 230, 80, 0.95), 
                        2.6
                    );
                }

                // Highlight reachable tiles with a blue overlay and attack targets with a red overlay
                if (reachable != null && reachable.contains(p)) {
                    drawFillOverlay(
                        g, 
                        tx, 
                        ty + 0.5, 
                        IsoGeometry.TILE_W - 3.0, 
                        IsoGeometry.TILE_H - 3.0, 
                        Color.rgb(70, 210, 255, 0.46)
                    );
                    drawStrokeOverlay(
                        g, 
                        tx, 
                        ty + 0.5, 
                        IsoGeometry.TILE_W - 3.0, 
                        IsoGeometry.TILE_H - 3.0, 
                        Color.rgb(120, 245, 255, 0.88), 
                        2.2
                    );
                }

                // Red overlay for attack targets
                if (attackTargets != null && attackTargets.contains(p)) {
                    drawFillOverlay(
                        g, 
                        tx, 
                        ty + 0.4, 
                        IsoGeometry.TILE_W - 3.0, 
                        IsoGeometry.TILE_H - 3.0, 
                        Color.rgb(255, 70, 70, 0.52)
                    );
                }

                // Highlight the selected unit's tile with a bright yellow stroke overlay
                if (selectedUnitPos != null && selectedUnitPos.equals(p)) {
                    drawStrokeOverlay(
                        g, 
                        tx, 
                        ty + 0.5, 
                        IsoGeometry.TILE_W - 2.0, 
                        IsoGeometry.TILE_H - 2.0, 
                        Color.rgb(255, 240, 120, 0.9), 
                        3.4
                    );
                }

                drawCaptureProgressBar(g, tile, tx, ty);

                // Render the unit on the tile, if present, with a colored circle and a health bar
                tile.getUnit().ifPresent(u -> {
                    double r = IsoGeometry.TILE_H * 0.55;
                    double cy = ty + IsoGeometry.TILE_H * 0.72;
                    g.setFill(unitColor(u.getPlayerId()));
                    g.fillOval(tx - r, cy - r, r * 2.0, r * 2.0);
                    g.setStroke(Color.rgb(0, 0, 0, 0.45));
                    g.setLineWidth(1.0);
                    g.strokeOval(tx - r, cy - r, r * 2.0, r * 2.0);

                    double hpRatio = Math.max(0.0, Math.min(1.0, u.getHp() / (double) u.getType().getMaxHp()));
                    double barW = 42.0;
                    double barH = 6.0;
                    double barX = tx - barW * 0.5;
                    double barY = cy - r - 10.0;

                    g.setFill(Color.rgb(20, 20, 20, 0.85));
                    g.fillRoundRect(barX, barY, barW, barH, 3.0, 3.0);
                    g.setFill(Color.rgb(80, 220, 120, 0.95));
                    g.fillRoundRect(barX + 1.0, barY + 1.0, Math.max(0.0, (barW - 2.0) * hpRatio), barH - 2.0, 2.0, 2.0);
                    g.setStroke(Color.rgb(0, 0, 0, 0.6));
                    g.setLineWidth(0.8);
                    g.strokeRoundRect(barX, barY, barW, barH, 3.0, 3.0);
                });
            }
        }

        g.restore();
    }

    // Helper method to draw a capture progress bar above HQ and Factory buildings that are being captured
    private void drawCaptureProgressBar(GraphicsContext g, Tile tile, double tx, double ty) {
        if (tile.getTerrain() != TerrainType.HQ && tile.getTerrain() != TerrainType.FACTORY)
            return;

        var buildingOpt = tile.getBuilding();
        if (buildingOpt.isEmpty())
            return;

        int capturePoints = buildingOpt.get().getCapturePoints();
        if (capturePoints >= 20)
            return;

        double progress = Math.max(0.0, Math.min(1.0, (20.0 - capturePoints) / 20.0));
        double barW = 42.0;
        double barH = 6.0;
        double barX = tx - barW * 0.5;
        double barY = ty - 8.0;

        g.setFill(Color.rgb(20, 20, 20, 0.88));
        g.fillRoundRect(barX, barY, barW, barH, 3.0, 3.0);
        g.setFill(Color.rgb(255, 190, 70, 0.96));
        g.fillRoundRect(barX + 1.0, barY + 1.0, Math.max(0.0, (barW - 2.0) * progress), barH - 2.0, 2.0, 2.0);
        g.setStroke(Color.rgb(0, 0, 0, 0.62));
        g.setLineWidth(0.8);
        g.strokeRoundRect(barX, barY, barW, barH, 3.0, 3.0);
    }
    
    // Helper methods for drawing various overlays with specified colors and line widths
    private void drawFillOverlay(GraphicsContext g, double tx, double ty, double width, double height, Color color) {
        g.setFill(color);
        IsoGeometry.fillDiamond(g, tx, ty, Math.max(0, width), Math.max(0, height));
    }

    private void drawStrokeOverlay(GraphicsContext g, double tx, double ty, double width, double height, Color color, double lineWidth) {
        g.setStroke(color);
        g.setLineWidth(lineWidth);
        g.setLineJoin(StrokeLineJoin.ROUND);
        IsoGeometry.strokeDiamond(g, tx, ty, Math.max(0, width), Math.max(0, height));
    }

    private Image resolveTerrainImage(Tile tile) {
        TerrainType terrain = tile.getTerrain();

        if (terrain == TerrainType.HQ) {
            Image variant = resolveOwnedVariant(tile, "HQ");
            if (variant != null)
                return variant;
        }

        if (terrain == TerrainType.FACTORY) {
            Image variant = resolveOwnedVariant(tile, "FACTORY");
            if (variant != null)
                return variant;
        }

        TerrainType spriteTerrain = terrain;
        Image img = sprites.terrain(spriteTerrain).orElse(null);
        if (img == null && spriteTerrain != TerrainType.PLAIN) {
            img = sprites.terrain(TerrainType.PLAIN).orElse(null);
        }
        return img;
    }

    private Image resolveOwnedVariant(Tile tile, String baseName) {
        String variant = tile.getBuilding()
            .map(b -> switch (b.getOwnerId()) {
                case 1 -> baseName + "_01";
                case 2 -> baseName + "_00";
                default -> null;
            })
            .orElse(null);
        if (variant == null)
            return null;

        Image image = sprites.terrain(variant).orElse(null);
        if (image != null)
            return image;
        return sprites.terrain(variant.replace("_", "")).orElse(null);
    }

    // Outdated method for determining terrain colors, kept as a fallback if sprites are missing

    private Color terrainColor(TerrainType terrain) {
        return switch (terrain) {
            case PLAIN -> Color.rgb(95, 170, 85);
            case FOREST -> Color.rgb(45, 125, 55);
            case MOUNTAIN -> Color.rgb(140, 140, 140);
            case CITY -> Color.rgb(190, 175, 130);
            case WATER -> Color.rgb(60, 110, 180);
            case FACTORY -> Color.rgb(160, 160, 165);
            case HQ -> Color.rgb(210, 150, 120);
        };
    }

    private Color unitColor(int playerId) {
        return switch (playerId) {
            case 1 -> Color.rgb(90, 170, 255);
            case 2 -> Color.rgb(255, 110, 110);
            default -> Color.rgb(220, 220, 220);
        };
    }
}
