package cx.rain.synccraft.sync.config;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class SyncConfigManager {
    private static final Gson GSON = new Gson();

    private File configFile;
    private SyncConfig config;

    public SyncConfigManager(Path dir) {
        configFile = new File(dir.toFile(), "sync_config.json");

        if (configFile.exists()) {
            try {
                config = GSON.fromJson(Files.readString(configFile.toPath()), SyncConfig.class);
            } catch (IOException ex) {
                throw new RuntimeException("Cannot read config file.");
            } catch (JsonSyntaxException ex) {
                configFile.delete();
                config = new SyncConfig();
                save(configFile, config);
            }
        } else {
            config = new SyncConfig();
            save(configFile, config);
        }
    }

    public SyncConfig getConfig() {
        return config;
    }

    private void save(File file, SyncConfig content) {
        try {
            FileUtils.write(file, GSON.toJson(content), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Cannot write config file.");
        }
    }
}
