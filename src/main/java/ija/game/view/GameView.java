package ija.game.view;

import ija.game.model.GameMap;
import ija.game.model.Position;
import ija.game.model.TerrainType;
import ija.game.model.Tile;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.image.Image;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.animation.AnimationTimer;

import java.nio.file.Path;
import java.util.Set;
import java.util.function.Consumer;

public class GameView extends BorderPane {

    // Isometric (2:1) diamond grid
    private static final double TILE_W = 96.0;
    private static final double TILE_H = 48.0;

    // 256x256 RGBA sprites- scaled down for the UI
    private static final double SPRITE_W = 96.0;
    private static final double SPRITE_H = 96.0;

    private static final double PADDING = 12.0;

    private static final double ZOOM_MIN = 0.45;
    private static final double ZOOM_MAX = 3.0;

    private final Canvas canvas;
    private final Label status;
    private final SpriteStore sprites;

    private final AnimationTimer redrawTimer;
    private boolean dirty = true;

    // Simple camera for pan/zoom.
    private double camX = 0.0;
    private double camY = 0.0;
    private double zoom = 1.0;
    private double lastPanX = 0.0;
    private double lastPanY = 0.0;
    private boolean panning = false;

    // Cached render state so camera interactions can trigger re-render.
    private GameMap lastMap;
    private Position lastSelectedUnitPos;
    private Set<Position> lastReachable;

    private Consumer<Position> onTileClicked;

    public GameView() {
        this.status = new Label("Ready");
        this.status.setPadding(new Insets(8));

        // Load full-res images (256x256) and let JavaFX downscale at draw time.
        // This keeps quality when zooming in (avoids upscaling an already downscaled texture).
        this.sprites = new SpriteStore(Path.of("lib/assets/sprites"));

        this.canvas = new Canvas(900, 650);

        StackPane center = new StackPane(canvas);
        center.setPadding(new Insets(PADDING));

        setCenter(center);
        setBottom(status);

        // Keep the canvas sized to the available center area.
        center.widthProperty().addListener((obs, o, n) -> resizeCanvas(center));
        center.heightProperty().addListener((obs, o, n) -> resizeCanvas(center));
        resizeCanvas(center);

        this.redrawTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (!dirty) {
                    return;
                }
                dirty = false;
                draw();
            }
        };
        this.redrawTimer.start();

        canvas.setOnMouseClicked(e -> {
            if (e.getButton() != MouseButton.PRIMARY) {
                return;
            }
            Position p = screenToTile(e.getX(), e.getY());
            if (onTileClicked != null) {
                onTileClicked.accept(p);
            }
        });

        // Pan with right mouse drag (keeps left click for gameplay interactions).
        canvas.setOnMousePressed(e -> {
            if (e.getButton() == MouseButton.SECONDARY) {
                panning = true;
                lastPanX = e.getX();
                lastPanY = e.getY();
                e.consume();
            }
        });
        canvas.setOnMouseReleased(e -> {
            if (e.getButton() == MouseButton.SECONDARY) {
                panning = false;
                e.consume();
            }
        });
        canvas.setOnMouseDragged(e -> {
            if (!panning) {
                return;
            }
            double dx = e.getX() - lastPanX;
            double dy = e.getY() - lastPanY;
            camX += dx;
            camY += dy;
            lastPanX = e.getX();
            lastPanY = e.getY();
            requestRedraw();
            e.consume();
        });

        // Zoom with mouse wheel, centered around the cursor position.
        canvas.setOnScroll(e -> {
            double delta = e.getDeltaY();
            if (delta == 0.0) {
                return;
            }

            // Smooth zoom that works well across mouse wheels and touchpads.
            // Typical mouse wheel delta is ~120; touchpads give smaller deltas.
            double factor = Math.pow(1.0015, delta);
            double newZoom = clamp(zoom * factor, ZOOM_MIN, ZOOM_MAX);
            if (newZoom == zoom) {
                return;
            }

            // Keep the world point under cursor stable when zooming.
            double worldXBefore = (e.getX() - camX) / zoom;
            double worldYBefore = (e.getY() - camY) / zoom;

            zoom = newZoom;

            camX = e.getX() - worldXBefore * zoom;
            camY = e.getY() - worldYBefore * zoom;

            requestRedraw();
            e.consume();
        });
    }

    public void setOnTileClicked(Consumer<Position> handler) {
        this.onTileClicked = handler;
    }

    public void setStatus(String text) {
        status.setText(text);
    }

    public void render(GameMap map, Position selectedUnitPos, Set<Position> reachable) {
        this.lastMap = map;
        this.lastSelectedUnitPos = selectedUnitPos;
        this.lastReachable = reachable;
        requestRedraw();
    }

    private void requestRedraw() {
        dirty = true;
    }

    private void resizeCanvas(StackPane center) {
        double w = center.getWidth() - center.getPadding().getLeft() - center.getPadding().getRight();
        double h = center.getHeight() - center.getPadding().getTop() - center.getPadding().getBottom();
        if (w < 1 || h < 1) {
            return;
        }
        canvas.setWidth(w);
        canvas.setHeight(h);
        requestRedraw();
    }

    private void draw() {
        if (lastMap == null) {
            return;
        }

        double cw = canvas.getWidth();
        double ch = canvas.getHeight();
        if (cw <= 1 || ch <= 1) {
            return;
        }

        GraphicsContext g = canvas.getGraphicsContext2D();
        g.setImageSmoothing(true);
        g.setFill(Color.rgb(18, 18, 20));
        g.fillRect(0, 0, cw, ch);

        // Visible bounds in world coordinates, with a small margin.
        double minWX = (-camX) / zoom - SPRITE_W;
        double minWY = (-camY) / zoom - SPRITE_H;
        double maxWX = (cw - camX) / zoom + SPRITE_W;
        double maxWY = (ch - camY) / zoom + SPRITE_H;

        g.save();
        g.translate(camX, camY);
        g.scale(zoom, zoom);

        double originX = PADDING;
        double originY = PADDING;

        int w = lastMap.getWidth();
        int h = lastMap.getHeight();

        for (int sum = 0; sum <= (w - 1) + (h - 1); sum++) {
            for (int y = 0; y < h; y++) {
                int x = sum - y;
                if (x < 0 || x >= w) {
                    continue;
                }

                double[] top = tileTop(originX, originY, x, y);
                double tx = top[0];
                double ty = top[1];

                // Cull tiles outside the viewport.
                double bbMinX = tx - SPRITE_W * 0.5;
                double bbMaxX = bbMinX + SPRITE_W;
                double bbMinY = ty + TILE_H - SPRITE_H;
                double bbMaxY = bbMinY + SPRITE_H;
                if (bbMaxX < minWX || bbMinX > maxWX || bbMaxY < minWY || bbMinY > maxWY) {
                    continue;
                }

                Tile tile = lastMap.getTile(x, y);

                // Terrain
                Image terrainImg = resolveTerrainImage(tile.getTerrain());
                if (terrainImg != null) {
                    g.drawImage(
                        terrainImg,
                        tx - SPRITE_W * 0.5,
                        ty + TILE_H - SPRITE_H,
                        SPRITE_W,
                        SPRITE_H
                    );
                } else {
                    g.setFill(terrainColor(tile.getTerrain()));
                    fillDiamond(g, tx, ty, TILE_W, TILE_H);
                }

                Position p = new Position(x, y);

                if (lastReachable != null && lastReachable.contains(p)) {
                    g.setFill(Color.rgb(80, 180, 255, 0.22));
                    fillDiamond(g, tx, ty + 2.0, Math.max(0, TILE_W - 4.0), Math.max(0, TILE_H - 4.0));
                }

                if (lastSelectedUnitPos != null && lastSelectedUnitPos.equals(p)) {
                    g.setStroke(Color.rgb(255, 240, 120, 0.9));
                    g.setLineWidth(2.5);
                    strokeDiamond(g, tx, ty + 1.5, Math.max(0, TILE_W - 3.0), Math.max(0, TILE_H - 3.0));
                }

                // Buildings
                tile.getBuilding().ifPresent(b -> {
                    Image img = sprites.building(b.getType()).orElse(null);
                    if (img == null) {
                        return;
                    }
                    g.drawImage(
                        img,
                        tx - SPRITE_W * 0.5,
                        ty + TILE_H - SPRITE_H,
                        SPRITE_W,
                        SPRITE_H
                    );
                });

                // Units: blob placeholders.
                tile.getUnit().ifPresent(u -> {
                    double r = TILE_H * 0.55;
                    double cy = ty + TILE_H * 0.72;
                    g.setFill(unitColor(u.getPlayerId()));
                    g.fillOval(tx - r, cy - r, r * 2.0, r * 2.0);
                    g.setStroke(Color.rgb(0, 0, 0, 0.45));
                    g.setLineWidth(1.0);
                    g.strokeOval(tx - r, cy - r, r * 2.0, r * 2.0);
                });
            }
        }

        g.restore();
    }

    private Image resolveTerrainImage(TerrainType terrain) {
        TerrainType spriteTerrain = switch (terrain) {
            case CITY, FACTORY, HQ -> TerrainType.PLAIN;
            default -> terrain;
        };
        Image img = sprites.terrain(spriteTerrain).orElse(null);
        if (img == null && spriteTerrain != TerrainType.PLAIN) {
            img = sprites.terrain(TerrainType.PLAIN).orElse(null);
        }
        return img;
    }

    private Position screenToTile(double x, double y) {
        // Convert from screen pixels to "world" coordinates (before we did translate/scale).
        double wx = (x - camX) / zoom;
        double wy = (y - camY) / zoom;

        double originX = PADDING;
        double originY = PADDING;

        double halfW = TILE_W * 0.5;
        double halfH = TILE_H * 0.5;
        double dx = wx - originX;
        double dy = wy - originY;

        int approxX = (int) Math.floor((dx / halfW + dy / halfH) * 0.5);
        int approxY = (int) Math.floor((dy / halfH - dx / halfW) * 0.5);

        // Refine by checking nearby diamonds.
        for (int yy = approxY - 2; yy <= approxY + 2; yy++) {
            for (int xx = approxX - 2; xx <= approxX + 2; xx++) {
                if (diamondContains(originX, originY, xx, yy, wx, wy)) {
                    return new Position(xx, yy);
                }
            }
        }

        return new Position(approxX, approxY);
    }

    private static double[] tileTop(double originX, double originY, int x, int y) {
        double halfW = TILE_W * 0.5;
        double halfH = TILE_H * 0.5;
        double sx = originX + (x - y) * halfW;
        double sy = originY + (x + y) * halfH;
        return new double[] { sx, sy };
    }

    private static boolean diamondContains(double originX, double originY, int tileX, int tileY, double px, double py) {
        double[] top = tileTop(originX, originY, tileX, tileY);
        double cx = top[0];
        double cy = top[1] + TILE_H * 0.5;
        double halfW = TILE_W * 0.5;
        double halfH = TILE_H * 0.5;

        double nx = Math.abs(px - cx) / halfW;
        double ny = Math.abs(py - cy) / halfH;
        return (nx + ny) <= 1.0;
    }

    private static void fillDiamond(GraphicsContext g, double topX, double topY, double w, double h) {
        double[] xs = new double[4];
        double[] ys = new double[4];
        diamondPoints(topX, topY, w, h, xs, ys);
        g.fillPolygon(xs, ys, 4);
    }

    private static void strokeDiamond(GraphicsContext g, double topX, double topY, double w, double h) {
        double[] xs = new double[4];
        double[] ys = new double[4];
        diamondPoints(topX, topY, w, h, xs, ys);
        g.strokePolygon(xs, ys, 4);
    }

    private static void diamondPoints(double topX, double topY, double w, double h, double[] xs, double[] ys) {
        double halfW = w * 0.5;
        double halfH = h * 0.5;
        xs[0] = topX;
        ys[0] = topY;
        xs[1] = topX + halfW;
        ys[1] = topY + halfH;
        xs[2] = topX;
        ys[2] = topY + h;
        xs[3] = topX - halfW;
        ys[3] = topY + halfH;
    }

    private static double clamp(double v, double min, double max) {
        if (v < min)
            return min;
        if (v > max)
            return max;
        return v;
    }

    private Color terrainColor(TerrainType t) {
        return switch (t) {
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
