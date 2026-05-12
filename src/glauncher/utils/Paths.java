package glauncher.utils;

import java.io.File;

public class Paths {
    public static final String DATA_DIR = (System.getenv("APPDATA") != null ? 
        System.getenv("APPDATA") : System.getProperty("user.home")) + File.separator + ".glauncher";
    
    public static final File VERSIONS_DIR = new File(DATA_DIR, "versions");
    public static final File SETTINGS_FILE = new File(DATA_DIR, "settings.json");
    public static final File SESSION_FILE = new File(DATA_DIR, "session.json");
    public static final File LIBS_DIR = new File(DATA_DIR, "libraries");
    public static final File ASSETS_DIR = new File(DATA_DIR, "assets");
    public static final File GMUSIC_DIR = new File(DATA_DIR, "gmusic_web");
}