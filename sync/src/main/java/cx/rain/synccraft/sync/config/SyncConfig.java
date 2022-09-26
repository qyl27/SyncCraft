package cx.rain.synccraft.sync.config;

public class SyncConfig implements ISyncConfig {
    public int configVersion = 1;

    public String directory = "synccraft";

    public String serverName = "SyncCraftExample";
    public String server = "http://localhost:35196/";   // Server URL.

    public String packVersion = "1.0.0";    // SemVer, for checking manifest.
}
