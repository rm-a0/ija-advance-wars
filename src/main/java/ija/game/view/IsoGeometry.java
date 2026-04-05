/**
 * Authors: Team xrepcim00
 * Description: Provides isometric coordinate transforms and diamond shape geometry helpers.
 */
package ija.game.view;

import ija.game.model.map.Position;

import javafx.scene.canvas.GraphicsContext;

public final class IsoGeometry {

    public static final double TILE_W = 116.0;
    public static final double TILE_H = 58.0;

    private IsoGeometry() {
    }

    // Converts screen coords to tile coords, accounting for camera pan and zoom, and the isometric projection.
    public static Position screenToTile(
        double screenX,
        double screenY,
        double camX,
        double camY,
        double zoom,
        double originX,
        double originY
    ) {
        double wx = (screenX - camX) / zoom;
        double wy = (screenY - camY) / zoom;

        double halfW = TILE_W * 0.5;
        double halfH = TILE_H * 0.5;
        double dx = wx - originX;
        double dy = wy - originY;

        int approxX = (int) Math.floor((dx / halfW + dy / halfH) * 0.5);
        int approxY = (int) Math.floor((dy / halfH - dx / halfW) * 0.5);

        for (int yy = approxY - 2; yy <= approxY + 2; yy++) {
            for (int xx = approxX - 2; xx <= approxX + 2; xx++) {
                if (diamondContains(originX, originY, xx, yy, wx, wy))
                    return new Position(xx, yy);
            }
        }

        return new Position(approxX, approxY);
    }

    // Gets the screen coordinates of the top point of the tile at (x, y) in tile coordinates.
    public static double[] tileTop(double originX, double originY, int x, int y) {
        double halfW = TILE_W * 0.5;
        double halfH = TILE_H * 0.5;
        double sx = originX + (x - y) * halfW;
        double sy = originY + (x + y) * halfH;
        return new double[] { sx, sy };
    }

    // Draws a filled diamond shape representing a tile at the given screen coordinates.
    public static void fillDiamond(GraphicsContext g, double topX, double topY, double w, double h) {
        double[] xs = new double[4];
        double[] ys = new double[4];
        diamondPoints(topX, topY, w, h, xs, ys);
        g.fillPolygon(xs, ys, 4);
    }

    // Draws a diamond outline representing a tile at the given screen coordinates.
    public static void strokeDiamond(GraphicsContext g, double topX, double topY, double w, double h) {
        double[] xs = new double[4];
        double[] ys = new double[4];
        diamondPoints(topX, topY, w, h, xs, ys);
        g.strokePolygon(xs, ys, 4);
    }

    // Checks if the point (px, py) in screen coordinates is within the diamond shape of the tile at (tileX, tileY).
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

    // Calculates the vertices of the diamond shape for a tile at (topX, topY) and stores them in the provided arrays.
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
}
