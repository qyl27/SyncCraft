package cx.rain.synccraft.sync.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class SyncConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private boolean server = false;

    private File configFile;
    private SyncConfig config;
    private File serverConfigFile;
    private SyncServerConfig serverConfig;
    private File langConfigFile;
    private SyncLanguageConfig langConfig;

    public SyncConfigManager(Path dir, boolean isServer) {
        server = isServer;

        configFile = new File(dir.toFile(), "synccraft/sync_config.json");
        serverConfigFile = new File(dir.toFile(), "synccraft/sync_server_config.json");
        langConfigFile = new File(dir.toFile(), "synccraft/sync_language_config.json");

        config = load(configFile, new SyncConfig());

        if (isServer) {
            serverConfig = load(serverConfigFile, new SyncServerConfig());
        }

        langConfig = load(langConfigFile, new SyncLanguageConfig());
    }

    public <T extends ISyncConfig> T load(File file, ISyncConfig defaultObj) {
        T config;
        if (file.exists()) {
            try {
                config = (T) GSON.fromJson(Files.readString(file.toPath()), defaultObj.getClass());
            } catch (IOException ex) {
                throw new RuntimeException("Cannot read config file.");
            } catch (JsonSyntaxException ex) {
                file.delete();
                config = (T) defaultObj;
                save(file, defaultObj);
            }
        } else {
            config = (T) defaultObj;
            save(file, config);
        }

        return config;
    }

    public boolean isServer() {
        return server;
    }

    public SyncConfig getConfig() {
        return config;
    }

    public boolean hasServerConfig() {
        return serverConfig != null;
    }

    public SyncServerConfig getServerConfig() {
        return serverConfig;
    }

    public SyncLanguageConfig getLanguage() {
        return langConfig;
    }

    private void save(File file, ISyncConfig content) {
        try {
            FileUtils.write(file, GSON.toJson(content), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Cannot write config file.");
        }
    }
}
