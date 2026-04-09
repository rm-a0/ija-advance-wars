/**
 * Authors: Team xrepcim00
 * Description: Main JavaFX game UI component with HUD, controls, and interactive map canvas.
 */
package ija.game.view;

import ija.game.model.map.GameMap;
import ija.game.model.map.Position;
import ija.game.model.unit.UnitType;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.animation.AnimationTimer;

import java.nio.file.Path;
import java.util.Set;
import java.util.function.Consumer;

public class GameView extends BorderPane {

    private static final double PADDING = 11.0;
    private static final String LIVE_STYLE = "-fx-background-color: #2c7a3f; -fx-text-fill: white; -fx-padding: 4 9 4 9; -fx-background-radius: 7; -fx-font-weight: bold;";
    private static final String REPLAY_STYLE = "-fx-background-color: #8c4f1b; -fx-text-fill: white; -fx-padding: 4 9 4 9; -fx-background-radius: 7; -fx-font-weight: bold;";

    private final Canvas canvas;
    private final Label status;
    private final Label fundsHud;
    private final Label sessionModeLabel;
    private final HBox factoryMenu;
    private final HBox sessionBar;
    private final StackPane gameOverOverlay;
    private final Label gameOverTitle;
    private final Label gameOverSubtitle;
    private final Button saveButton;
    private final Button loadButton;
    private final Button loadReplayButton;
    private final Button prevReplayButton;
    private final Button nextReplayButton;
    private final Button returnLiveButton;
    private final Button botToggleButton;
    private final SpriteStore sprites;
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
    private Set<Position> lastAttackTargets;
    private Position focusedTilePos;
    private Position hoveredTilePos;

    private Consumer<Position> onTileClicked;
    private Runnable onBuyInfantry;
    private Runnable onBuyTank;
    private Runnable onBuyArtillery;
    private Runnable onSaveGame;
    private Runnable onLoadGame;
    private Runnable onReplayLoad;
    private Runnable onReplayPrev;
    private Runnable onReplayNext;
    private Runnable onReplayLive;
    private Runnable onToggleBot;

    public GameView() {
        // Initialize UI components and layout
        this.status = new Label("Ready");
        this.status.setPadding(new Insets(8));
        this.fundsHud = new Label("Player 1 | Turn 1 | Funds 3000");
        this.fundsHud.setStyle("-fx-background-color: rgba(0,0,0,0.55); -fx-text-fill: white; -fx-padding: 6 10 6 10; -fx-background-radius: 8;");
        this.sessionModeLabel = new Label("LIVE");
        this.sessionModeLabel.setStyle(LIVE_STYLE);

        // Load full-res images (256x256)
        // This keeps quality when zooming in (avoids upscaling an already downscaled texture).
        this.sprites = new SpriteStore(Path.of("lib/assets/sprites"));
        this.boardRenderer = new BoardRenderer(sprites);
        this.camera = new IsometricCamera();

        // Init main canvas and UI controls, and set up layout
        this.canvas = new Canvas(900, 650);
        this.factoryMenu = createFactoryMenu();
        this.gameOverTitle = new Label("Player 1 Wins");
        this.gameOverSubtitle = new Label("HQ captured.");
        this.gameOverOverlay = createGameOverOverlay();
        this.saveButton = createSessionButton("Save", () -> runIfSet(onSaveGame));
        this.loadButton = createSessionButton("Load", () -> runIfSet(onLoadGame));
        this.loadReplayButton = createSessionButton("Replay", () -> runIfSet(onReplayLoad));
        this.prevReplayButton = createSessionButton("<", () -> runIfSet(onReplayPrev));
        this.nextReplayButton = createSessionButton(">", () -> runIfSet(onReplayNext));
        this.returnLiveButton = createSessionButton("Live", () -> runIfSet(onReplayLive));
        this.botToggleButton = createSessionButton("Bot OFF", () -> runIfSet(onToggleBot));
        this.sessionBar = createSessionBar();

        // Create a StackPane for the center area to layer the canvas, HUD, and menus on top of each other
        StackPane center = new StackPane(canvas);
        center.setPadding(new Insets(PADDING));
        center.getChildren().add(fundsHud);
        center.getChildren().add(factoryMenu);
        center.getChildren().add(sessionBar);
        center.getChildren().add(gameOverOverlay);
        StackPane.setAlignment(fundsHud, Pos.TOP_RIGHT);
        StackPane.setMargin(fundsHud, new Insets(10, 12, 0, 0));
        StackPane.setAlignment(factoryMenu, Pos.BOTTOM_CENTER);
        StackPane.setMargin(factoryMenu, new Insets(0, 0, 8, 0));
        StackPane.setAlignment(sessionBar, Pos.TOP_LEFT);
        StackPane.setMargin(sessionBar, new Insets(10, 0, 0, 12));
        StackPane.setAlignment(gameOverOverlay, Pos.CENTER);

        // Close factory menu when user clicks anywhere outside the menu itself.
        center.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            if (!factoryMenu.isVisible())
                return;
            if (isInsideFactoryMenu(e))
                return;
            hideFactoryMenu();
        });

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

        // Handle mouse clicks for tile selection and interactions, and mouse movement for hover effects
        canvas.setOnMouseClicked(e -> {
            if (e.getButton() != MouseButton.PRIMARY)
                return;
            Position p = camera.screenToTile(e.getX(), e.getY(), PADDING, PADDING);
            focusedTilePos = p;
            requestRedraw();
            if (onTileClicked != null) onTileClicked.accept(p);
        });

        canvas.setOnMouseMoved(e -> {
            Position p = camera.screenToTile(e.getX(), e.getY(), PADDING, PADDING);
            if (lastMap != null && !lastMap.isInBounds(p))
                p = null;
            if ((hoveredTilePos == null && p == null) || (hoveredTilePos != null && hoveredTilePos.equals(p)))
                return;
            hoveredTilePos = p;
            requestRedraw();
        });

        canvas.setOnMouseExited(e -> {
            hoveredTilePos = null;
            requestRedraw();
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

        setSessionMode(false);
        setGameOver(false, -1);
    }

    // Public API for the controller to interact with the view

    public void setOnTileClicked(Consumer<Position> handler) {
        this.onTileClicked = handler;
    }

    public void setStatus(String text) {
        status.setText(text);
    }

    public void setHud(int currentPlayerId, int turnNumber, int funds) {
        fundsHud.setText("Player " + currentPlayerId + " | Turn " + turnNumber + " | Funds " + funds);
    }

    public void setOnBuyInfantry(Runnable onBuyInfantry) {
        this.onBuyInfantry = onBuyInfantry;
    }

    public void setOnBuyTank(Runnable onBuyTank) {
        this.onBuyTank = onBuyTank;
    }

    public void setOnBuyArtillery(Runnable onBuyArtillery) {
        this.onBuyArtillery = onBuyArtillery;
    }

    public void setOnSaveGame(Runnable onSaveGame) {
        this.onSaveGame = onSaveGame;
    }

    public void setOnLoadGame(Runnable onLoadGame) {
        this.onLoadGame = onLoadGame;
    }

    public void setOnReplayLoad(Runnable onReplayLoad) {
        this.onReplayLoad = onReplayLoad;
    }

    public void setOnReplayPrev(Runnable onReplayPrev) {
        this.onReplayPrev = onReplayPrev;
    }

    public void setOnReplayNext(Runnable onReplayNext) {
        this.onReplayNext = onReplayNext;
    }

    public void setOnReplayLive(Runnable onReplayLive) {
        this.onReplayLive = onReplayLive;
    }

    public void setOnToggleBot(Runnable onToggleBot) {
        this.onToggleBot = onToggleBot;
    }

    public void setBotEnabled(boolean enabled) {
        botToggleButton.setText(enabled ? "Bot ON" : "Bot OFF");
    }

    public void showFactoryMenu() {
        factoryMenu.setVisible(true);
        factoryMenu.setManaged(true);
    }

    public void hideFactoryMenu() {
        factoryMenu.setVisible(false);
        factoryMenu.setManaged(false);
    }

    public boolean isFactoryMenuVisible() {
        return factoryMenu.isVisible();
    }

    public void setGameOver(boolean gameOver, int winnerId) {
        if (!gameOver) {
            gameOverOverlay.setVisible(false);
            gameOverOverlay.setManaged(false);
            return;
        }

        gameOverTitle.setText("Player " + winnerId + " Wins");
        gameOverSubtitle.setText("HQ captured. Replay or load a game to continue.");
        gameOverOverlay.setVisible(true);
        gameOverOverlay.setManaged(true);
    }

    // Sets the session mode (live vs replay) and updates the UI controls accordingly.
    public void setSessionMode(boolean replayMode) {
        sessionModeLabel.setText(replayMode ? "REPLAY" : "LIVE");
        sessionModeLabel.setStyle(replayMode ? REPLAY_STYLE : LIVE_STYLE);

        saveButton.setDisable(replayMode);
        loadButton.setDisable(replayMode);
        loadReplayButton.setDisable(replayMode);
        botToggleButton.setDisable(replayMode);
        prevReplayButton.setDisable(!replayMode);
        nextReplayButton.setDisable(!replayMode);
        returnLiveButton.setDisable(!replayMode);
    }

    // Renders the game map and highlights based on the current camera view and interaction state.
    public void render(GameMap map, Position selectedUnitPos, Set<Position> reachable, Set<Position> attackTargets) {
        this.lastMap = map;
        this.lastSelectedUnitPos = selectedUnitPos;
        this.lastReachable = reachable;
        this.lastAttackTargets = attackTargets;
        requestRedraw();
    }

    private void requestRedraw() {
        dirty = true;
    }

    // Resizes the canvas to fit the available space in the center area, accounting for padding.
    private void resizeCanvas(StackPane center) {
        double w = center.getWidth() - center.getPadding().getLeft() - center.getPadding().getRight();
        double h = center.getHeight() - center.getPadding().getTop() - center.getPadding().getBottom();
        if (w < 1 || h < 1)
            return;
        canvas.setWidth(w);
        canvas.setHeight(h);
        requestRedraw();
    }

    // Draws the game map and highlights on the canvas based on the last rendered state and current camera settings.
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
            lastAttackTargets,
            focusedTilePos,
            hoveredTilePos,
            camera,
            cw,
            ch,
            PADDING,
            PADDING
        );
    }

    private HBox createFactoryMenu() {
        HBox box = new HBox(10);
        box.setPadding(new Insets(6));
        box.setStyle("-fx-background-color: rgba(20,20,22,0.88); -fx-background-radius: 10;");
        box.setAlignment(Pos.CENTER);
        box.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        box.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        box.setPickOnBounds(false);

        Button infantry = createBuyButton(UnitType.INFANTRY, () -> {
            if (onBuyInfantry != null)
                onBuyInfantry.run();
        });
        Button tank = createBuyButton(UnitType.TANK, () -> {
            if (onBuyTank != null)
                onBuyTank.run();
        });
        Button artillery = createBuyButton(UnitType.ARTILLERY, () -> {
            if (onBuyArtillery != null)
                onBuyArtillery.run();
        });

        box.getChildren().addAll(infantry, tank, artillery);
        box.setVisible(false);
        box.setManaged(false);
        return box;
    }

    private HBox createSessionBar() {
        HBox box = new HBox(6);
        box.setPadding(new Insets(6));
        box.setStyle("-fx-background-color: rgba(20,20,22,0.88); -fx-background-radius: 10;");
        box.setAlignment(Pos.CENTER_LEFT);
        box.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        box.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        box.setPickOnBounds(false);

        box.getChildren().addAll(
            sessionModeLabel,
            saveButton,
            loadButton,
            loadReplayButton,
            botToggleButton,
            prevReplayButton,
            nextReplayButton,
            returnLiveButton
        );
        return box;
    }

    private StackPane createGameOverOverlay() {
        gameOverTitle.setStyle("-fx-text-fill: #ffd991; -fx-font-size: 34px; -fx-font-weight: bold;");
        gameOverSubtitle.setStyle("-fx-text-fill: #f7f3e7; -fx-font-size: 15px;");

        VBox panel = new VBox(10, gameOverTitle, gameOverSubtitle);
        panel.setAlignment(Pos.CENTER);
        panel.setPadding(new Insets(28));
        panel.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        panel.setStyle(
            "-fx-background-color: rgba(24,18,12,0.95);"
            + "-fx-background-radius: 14;"
            + "-fx-border-color: #d6a658;"
            + "-fx-border-width: 2;"
            + "-fx-border-radius: 14;"
        );

        StackPane overlay = new StackPane(panel);
        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.62);");
        overlay.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        overlay.setPickOnBounds(true);
        overlay.setVisible(false);
        overlay.setManaged(false);
        return overlay;
    }

    private Button createSessionButton(String text, Runnable action) {
        Button button = new Button(text);
        button.setPrefSize(72, 32);
        button.setMinSize(72, 32);
        button.setMaxSize(72, 32);
        button.setStyle("-fx-background-color: #7a7a7a; -fx-background-radius: 8; -fx-border-color: #4e4e4e; -fx-border-radius: 8; -fx-text-fill: white;");
        button.setOnAction(e -> action.run());
        return button;
    }

    private static void runIfSet(Runnable action) {
        if (action != null)
            action.run();
    }

    private Button createBuyButton(UnitType unitType, Runnable action) {
        Image image = resolveFactoryUnitImage(unitType);
        ImageView sprite = new ImageView();
        sprite.setFitWidth(42);
        sprite.setFitHeight(42);
        sprite.setPreserveRatio(true);
        if (image != null)
            sprite.setImage(image);

        Button button = new Button();
        button.setGraphic(sprite);
        button.setPrefSize(60, 60);
        button.setMinSize(60, 60);
        button.setMaxSize(60, 60);
        button.setStyle("-fx-background-color: #7a7a7a; -fx-background-radius: 8; -fx-border-color: #4e4e4e; -fx-border-radius: 8;");
        button.setOnAction(e -> action.run());
        return button;
    }

    // Temporary mapping: infantry/artillery use tank icon until dedicated sprites are added.
    private Image resolveFactoryUnitImage(UnitType unitType) {
        String baseName = switch (unitType) {
            case INFANTRY -> "tank";
            case TANK -> "tank";
            case ARTILLERY -> "tank";
        };

        Image image = sprites.unit(baseName + "_01").orElse(null);
        if (image != null)
            return image;

        image = sprites.unit(baseName + "_02").orElse(null);
        if (image != null)
            return image;

        return sprites.unit(UnitType.TANK).orElse(null);
    }

    private boolean isInsideFactoryMenu(MouseEvent e) {
        if (factoryMenu.getScene() == null)
            return false;
        var bounds = factoryMenu.localToScene(factoryMenu.getBoundsInLocal());
        return bounds != null && bounds.contains(e.getSceneX(), e.getSceneY());
    }

}
