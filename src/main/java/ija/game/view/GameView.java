package ija.game.view;

import ija.game.model.GameMap;
import ija.game.model.Position;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.animation.AnimationTimer;

import java.nio.file.Path;
import java.util.Set;
import java.util.function.Consumer;

public class GameView extends BorderPane {

    private static final double PADDING = 11.0;

    private final Canvas canvas;
    private final Label status;
    private final BoardRenderer boardRenderer;
    private final IsometricCamera camera;

    private final AnimationTimer redrawTimer;
    private boolean dirty = true;

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
        SpriteStore sprites = new SpriteStore(Path.of("lib/assets/sprites"));
        this.boardRenderer = new BoardRenderer(sprites);
        this.camera = new IsometricCamera();

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
                if (!dirty)
                    return;
                dirty = false;
                draw();
            }
        };
        this.redrawTimer.start();

        canvas.setOnMouseClicked(e -> {
            if (e.getButton() != MouseButton.PRIMARY)
                return;
            Position p = camera.screenToTile(e.getX(), e.getY(), PADDING, PADDING);
            if (onTileClicked != null)
                onTileClicked.accept(p);
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
            if (!panning)
                return;
            double dx = e.getX() - lastPanX;
            double dy = e.getY() - lastPanY;
            camera.panBy(dx, dy);
            lastPanX = e.getX();
            lastPanY = e.getY();
            requestRedraw();
            e.consume();
        });

        // Zoom with mouse wheel, centered around the cursor position.
        canvas.setOnScroll(e -> {
            if (camera.zoomAt(e.getDeltaY(), e.getX(), e.getY())) {
                requestRedraw();
                e.consume();
            }
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
        if (w < 1 || h < 1)
            return;
        canvas.setWidth(w);
        canvas.setHeight(h);
        requestRedraw();
    }

    private void draw() {
        if (lastMap == null)
            return;

        double cw = canvas.getWidth();
        double ch = canvas.getHeight();
        if (cw <= 1 || ch <= 1)
            return;

        GraphicsContext g = canvas.getGraphicsContext2D();
        boardRenderer.draw(
            g,
            lastMap,
            lastSelectedUnitPos,
            lastReachable,
            camera,
            cw,
            ch,
            PADDING,
            PADDING
        );
    }

}
