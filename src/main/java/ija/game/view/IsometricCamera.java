package ija.game.view;

import ija.game.model.Position;

public class IsometricCamera {

    private static final double ZOOM_MIN = 0.45;
    private static final double ZOOM_MAX = 3.0;

    private double camX;
    private double camY;
    private double zoom;

    public IsometricCamera() {
        this.camX = 0.0;
        this.camY = 0.0;
        this.zoom = 1.0;
    }

    public void panBy(double dx, double dy) {
        camX += dx;
        camY += dy;
    }

    public boolean zoomAt(double delta, double screenX, double screenY) {
        if (delta == 0.0) {
            return false;
        }

        double factor = Math.pow(1.0015, delta);
        double newZoom = clamp(zoom * factor, ZOOM_MIN, ZOOM_MAX);
        if (newZoom == zoom) {
            return false;
        }

        double worldXBefore = (screenX - camX) / zoom;
        double worldYBefore = (screenY - camY) / zoom;

        zoom = newZoom;
        camX = screenX - worldXBefore * zoom;
        camY = screenY - worldYBefore * zoom;

        return true;
    }

    public Position screenToTile(double screenX, double screenY, double originX, double originY) {
        return IsoGeometry.screenToTile(screenX, screenY, camX, camY, zoom, originX, originY);
    }

    public double getCamX() {
        return camX;
    }

    public double getCamY() {
        return camY;
    }

    public double getZoom() {
        return zoom;
    }

    private static double clamp(double v, double min, double max) {
        if (v < min) {
            return min;
        }
        if (v > max) {
            return max;
        }
        return v;
    }
}
