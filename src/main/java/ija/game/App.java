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

        GameState state = MapLoader.loadMap(mapPath);
        GameLogService logService = GameLogService.startSession(Path.of("data/logs"), state);

        GameView view = new GameView();
        GameController controller = new GameController(state, view, logService);

        Scene scene = new Scene(view, 980, 760);

        Runnable toggleFullscreen = () -> stage.setFullScreen(!stage.isFullScreen());
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.F11), toggleFullscreen);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.ENTER, KeyCombination.ALT_DOWN), toggleFullscreen);

        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.F11 || (e.getCode() == KeyCode.ENTER && e.isAltDown())) {
                toggleFullscreen.run();
                e.consume();
                return;
            }
            if (e.getCode() == KeyCode.E) {
                controller.endTurn();
                e.consume();
                return;
            }
            if (e.getCode() == KeyCode.DIGIT1) {
                controller.buyInfantry();
                e.consume();
                return;
            }
            if (e.getCode() == KeyCode.DIGIT2) {
                controller.buyTank();
                e.consume();
                return;
            }
            if (e.getCode() == KeyCode.DIGIT3) {
                controller.buyArtillery();
                e.consume();
            }
        });

        stage.setResizable(true);
        stage.setTitle("IJA Game v1.0");
        stage.setScene(scene);
        stage.show();
    }
}