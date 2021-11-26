package al.yn.synccraft;

import al.yn.synccraft.data.Config;
import al.yn.synccraft.data.ModManifest;
import com.google.gson.Gson;
import cpw.mods.modlauncher.Launcher;
import cpw.mods.modlauncher.api.IEnvironment;
import net.minecraftforge.forgespi.Environment;
import net.minecraftforge.forgespi.locating.IModFile;
import net.minecraftforge.forgespi.locating.IModLocator;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class SyncLocator implements IModLocator {
    private static final Logger LOG = LogManager.getLogger("SyncLocator");
    private static final Gson GSON = new Gson();

    private Path gameDirectory;
    private File configFile;
    private Config config;

    private Path modDirectory;

    private ModManifest modManifest;

    private IModLocator origin;

    public SyncLocator() throws Exception {
        // Load configs.
        gameDirectory = Launcher.INSTANCE.environment()
                .getProperty(IEnvironment.Keys.GAMEDIR.get())
                .orElse(Paths.get("."));

        configFile = new File(gameDirectory.toFile(), "sync_config.json");

        if (configFile.exists()) {
            config = GSON.fromJson(Files.readString(configFile.toPath()), Config.class);
        } else {
            config = new Config();
            FileUtils.write(configFile, GSON.toJson(config), StandardCharsets.UTF_8);
        }

        modDirectory = gameDirectory.resolve(config.directory);
        if (!modDirectory.toFile().exists()) {
            modDirectory.toFile().mkdirs();
        }

        // Load mod manifest.
        try (var stream = new URL(config.server + "/mod_manifest.json").openStream()) {
            var manifestString = IOUtils.toString(stream, StandardCharsets.UTF_8);
            modManifest = GSON.fromJson(manifestString, ModManifest.class);
        }

        if (!config.serverName.equals(modManifest.name)) {
            throw new Exception("Mismatched server name.");
        }

        // Check mod lists.
        var modsLocal = modDirectory.toFile().listFiles();

        var modsOnServer = new ArrayList<>(Arrays.stream(modManifest.mods).toList());
        var modsToDownload = new ArrayList<>(modsOnServer);

        for (var modFile : Arrays.stream(modsLocal).toList()) {
            // Remove mismatched mod files.
            if (modsOnServer.stream().noneMatch(m -> {
                try {
                    if (m.name.equals(modFile.getName())
                            && m.checksum.equalsIgnoreCase(
                            DigestUtils.sha256Hex(FileUtils.readFileToByteArray(modFile)))) {
                        modsToDownload.remove(m);
                        return true;
                    } else {
                        return false;
                    }
                } catch (IOException ignored) {
                    return false;
                }
            })) {
                var result = modFile.delete();
                if (!result) {
                    throw new Exception("Cannot delete mismatched mod file " + modFile.getName() + " .");
                }
            }
        }

        for (var mod : modsToDownload) {
            // Download mods.
            FileUtils.copyURLToFile(new URL(config.server + "/mods/" + mod.name),
                    new File(modDirectory.toFile(), mod.name));
        }

    }

    @Override
    public List<IModFile> scanMods() {
        return origin.scanMods();
    }

    @Override
    public String name() {
        return "SyncCraft";
    }

    @Override
    public void scanFile(IModFile modFile, Consumer<Path> pathConsumer) {
        origin.scanFile(modFile, pathConsumer);
    }

    @Override
    public void initArguments(Map<String, ?> arguments) {
        origin = Launcher.INSTANCE.environment()
                .getProperty(Environment.Keys.MODDIRECTORYFACTORY.get())
                .orElseThrow(() -> new RuntimeException("There is no Mod Directory Factory!"))
                .build(modDirectory, "Syncing Mods Directory");
    }

    @Override
    public boolean isValid(IModFile modFile) {
        return !Arrays.stream(modManifest.mods).noneMatch(m -> {
            try {
                return m.name.equals(modFile.getFileName())
                        && m.checksum.equalsIgnoreCase(
                        DigestUtils.sha256Hex(FileUtils.readFileToByteArray(modFile.getFilePath().toFile())));
            } catch (IOException ignored) {
                return false;
            }
        });
    }
}
