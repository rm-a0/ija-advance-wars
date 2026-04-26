/**
 * Authors: Team xrepcim00
 * Description: Application entry point. 
 * Bootstraps data loading and launches the game.
 */
package ija.game;

import ija.game.controller.GameController;
import ija.game.io.DataLoader;
import ija.game.io.GameLogService;
import ija.game.io.MapLoader;
import ija.game.model.state.GameState;

import ija.game.view.GameView;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.stage.Stage;

import java.nio.file.Path;

public class App extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    // App entry point
    @Override
    public void start(Stage stage) {
        // Load game data from TSV (required before any enum stats are used).
        // Load prepared data from the root-level data/ directory.
        DataLoader.loadAll("data");

        // Pick a map (arg0 overrides default)
        String mapPath = "data/maps/ui_test_map.json";
        var params = getParameters();
        if (!params.getRaw().isEmpty())
            mapPath = params.getRaw().getFirst();

        // Load map and create initial game state
        GameState state = MapLoader.loadMap(mapPath);
        GameLogService logService = GameLogService.startSession(Path.of("data/logs"), state);

        // Create view and controller, and bind them together
        GameView view = new GameView();
        GameController controller = new GameController(state, view, logService);

        // Show the UI
        Scene scene = new Scene(view, 980, 760);

        // Global keybindings
        Runnable toggleFullscreen = () -> {
            stage.setFullScreen(!stage.isFullScreen());
            Platform.runLater(view::refreshOverlayLayout);
        };
        view.setOnToggleFullscreen(toggleFullscreen);
        stage.fullScreenProperty().addListener((obs, oldVal, newVal) -> Platform.runLater(view::refreshOverlayLayout));
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.F11), toggleFullscreen);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.ENTER, KeyCombination.ALT_DOWN), toggleFullscreen);

        scene.setOnKeyPressed(e -> {
            if (isFullscreenToggle(e.getCode(), e.isAltDown())) {
                toggleFullscreen.run();
                e.consume();
                return;
            }

            switch (e.getCode()) {
                case E -> controller.endTurn();
                case DIGIT1 -> controller.buyInfantry();
                case DIGIT2 -> controller.buyTank();
                case DIGIT3 -> controller.buyArtillery();
                default -> {
                    return;
                }
            }
            e.consume();
        });
        
        stage.setResizable(true);
        stage.setTitle("IJA Game v1.0");
        stage.setScene(scene);
        stage.show();
    }

    private static boolean isFullscreenToggle(KeyCode code, boolean altDown) {
        return code == KeyCode.F11 || (code == KeyCode.ENTER && altDown);
    }
}