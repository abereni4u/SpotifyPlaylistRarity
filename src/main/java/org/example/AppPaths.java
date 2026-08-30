package org.example;

/*
 * -- Overview --
 * Single place that controls every filesystem path the app touches.
 *
 *   Windows -> C:\Users\<user>\AppData\Roaming\PlaylistRarity
 *   macOS   -> ~/Library/Application Support/PlaylistRarity
 *   Linux   -> $XDG_CONFIG_HOME/PlaylistRarity  (or ~/.config/PlaylistRarity)
 */

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class AppPaths {

    private static final String APP_FOLDER = "PlaylistRarity";

    // base config dir, OS dependent. Nothing is created here, callers that write use csvDir()
    // or create the parent themselves
    public static Path configDir(){
        String os = System.getProperty("os.name").toLowerCase();
        String userHome = System.getProperty("user.home");

        if (os.contains("mac")){
            return Paths.get(userHome, "Library", "Application Support", APP_FOLDER);
        }
        else if (os.contains("win")){
            return Paths.get(System.getenv("APPDATA"), APP_FOLDER);
        }
        else {
            // Linux and other Unix-likes
            String xdgConfig = System.getenv("XDG_CONFIG_HOME");
            if (xdgConfig != null && !xdgConfig.isBlank()){
                return Paths.get(xdgConfig, APP_FOLDER);
            }
            return Paths.get(userHome, ".config", APP_FOLDER);
        }
    }

    public static Path tokenFile(){
        return configDir().resolve("tokens.json");
    }

    public static Path csvDir() throws IOException {
        Path dir = configDir().resolve("playlists");
        Files.createDirectories(dir);
        return dir;
    }


    public static Path downloadsDir(){
        return Paths.get(System.getProperty("user.home"), "Downloads");
    }
}
