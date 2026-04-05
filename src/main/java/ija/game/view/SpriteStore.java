/**
 * Authors: Team xrepcim00
 * Description: Loads and caches terrain and unit sprites from the asset directory.
 */
package ija.game.view;

import ija.game.model.map.TerrainType;
import ija.game.model.unit.UnitType;

import javafx.scene.image.Image;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class SpriteStore {

    private final Path spritesRoot;
    private final double requestedWidth;
    private final double requestedHeight;
    private final boolean preserveRatio;
    private final boolean smooth;
    private final Map<String, Optional<Image>> cache = new HashMap<>();

    public SpriteStore(Path spritesRoot) {
        this(spritesRoot, 0, 0, true, true);
    }

    public SpriteStore(Path spritesRoot, double requestedWidth, double requestedHeight, boolean preserveRatio, boolean smooth) {
        this.spritesRoot = spritesRoot;
        this.requestedWidth = requestedWidth;
        this.requestedHeight = requestedHeight;
        this.preserveRatio = preserveRatio;
        this.smooth = smooth;
    }

    public Optional<Image> terrain(TerrainType type) {
        return load("terrain", type.name());
    }

    public Optional<Image> terrain(String spriteKey) {
        if (spriteKey == null || spriteKey.isBlank())
            return Optional.empty();
        return load("terrain", spriteKey);
    }

    public Optional<Image> unit(UnitType type) {
        return load("units", type.name());
    }

    private Optional<Image> load(String category, String enumName) {
        String key = category + ":" + enumName;
        return cache.computeIfAbsent(key, ignored -> tryLoad(category, enumName));
    }

    private Optional<Image> tryLoad(String category, String enumName) {
        if (spritesRoot == null)
            return Optional.empty();

        String lower = enumName.toLowerCase(Locale.ROOT);

        Path p1 = spritesRoot.resolve(category).resolve(lower + ".png");
        Path p2 = spritesRoot.resolve(category).resolve(enumName + ".png");

        Image img = loadImageIfExists(p1);
        if (img != null)
            return Optional.of(img);

        img = loadImageIfExists(p2);
        if (img != null)
            return Optional.of(img);

        return Optional.empty();
    }

    private Image loadImageIfExists(Path path) {
        if (path == null || !Files.exists(path) || !Files.isRegularFile(path))
            return null;

        String uri = path.toUri().toString();
        if (requestedWidth > 0 && requestedHeight > 0)
            return new Image(uri, requestedWidth, requestedHeight, preserveRatio, smooth, false);
        return new Image(uri, false);
    }
}
