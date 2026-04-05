/**
 * Authors: Team xrepcim00
 * Description: Manages pan, zoom, and screen-to-tile conversion parameters for rendering.
 */
package ija.game.view;

import ija.game.model.map.Position;

public class IsometricCamera {

    private static final double ZOOM_MIN = 0.45;
    private static final double ZOOM_MAX = 3.0;

    private double camX;
    private double camY;
    private double zoom;

    // Initializes the camera with default pan (0, 0) and zoom (1.0).
    public IsometricCamera() {
        this.camX = 0.0;
        this.camY = 0.0;
        this.zoom = 1.0;
    }

    // Pans the camera by the specified amounts in screen coordinates.
    public void panBy(double dx, double dy) {
        camX += dx;
        camY += dy;
    }

    // Zooms the camera in or out by the specified delta, centered on the given screen coordinates.
    public boolean zoomAt(double delta, double screenX, double screenY) {
        if (delta == 0.0)
            return false;

        double factor = Math.pow(1.0015, delta);
        double newZoom = clamp(zoom * factor, ZOOM_MIN, ZOOM_MAX);
        if (newZoom == zoom)
            return false;

        double worldXBefore = (screenX - camX) / zoom;
        double worldYBefore = (screenY - camY) / zoom;

        zoom = newZoom;
        camX = screenX - worldXBefore * zoom;
        camY = screenY - worldYBefore * zoom;

        return true;
    }

    // Converts screen coords to tile coords, accounting for camera pan and zoom, and the isometric projection.
    public Position screenToTile(double screenX, double screenY, double originX, double originY) {
        return IsoGeometry.screenToTile(screenX, screenY, camX, camY, zoom, originX, originY);
    }

    public double getCamX() { return camX; }

    public double getCamY() { return camY; }

    public double getZoom() { return zoom; }

    private static double clamp(double v, double min, double max) {
        if (v < min)
            return min;
        if (v > max)
            return max;
        return v;
    }
}
