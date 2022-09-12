package cx.rain.synccraft.sync;

import cpw.mods.modlauncher.Launcher;
import cpw.mods.modlauncher.api.IEnvironment;
import cx.rain.synccraft.Constants;
import cx.rain.synccraft.sync.config.SyncConfigManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.forgespi.Environment;
import net.minecraftforge.forgespi.locating.IModFile;
import net.minecraftforge.forgespi.locating.IModLocator;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class SyncLocator implements IModLocator {
    private static final Logger LOGGER = LoggerFactory.getLogger(Constants.NAME);

    private Path gameDirectory;
    private SyncConfigManager configManager;

    private IModLocator origin;
    private boolean shouldLoadSyncedMods = false;

    public SyncLocator() throws Exception {
        LOGGER.info("Initializing SyncCraft ver:" + Constants.VERSION);

        var dist = Launcher.INSTANCE.environment().getProperty(Environment.Keys.DIST.get());
        assert dist != null && dist.isPresent();
        if (dist.get() == Dist.CLIENT) {
            onClient();
        } else {
            LOGGER.error("Not supported server yet.");
            throw new RuntimeException("Not supported server yet.");
        }
    }

    private void onClient() throws Exception {
        // Load configs.
        gameDirectory = Launcher.INSTANCE.environment()
                .getProperty(IEnvironment.Keys.GAMEDIR.get())
                .orElse(Paths.get("."));

        configManager = new SyncConfigManager(gameDirectory);

        var url = configManager.getConfig().server;
        if (!url.endsWith("/")) {
            url += "/";
        }
        url += "mods.json";

        var manifestString = IOUtils.toString(new URL(url), StandardCharsets.UTF_8);
        // Todo.
    }

    @Override
    public List<ModFileOrException> scanMods() {
        if (shouldLoadSyncedMods) {
            return origin.scanMods();
        } else {
            return new ArrayList<>();
        }
    }

    @Override
    public String name() {
        return Constants.NAME;
    }

    @Override
    public void scanFile(IModFile modFile, Consumer<Path> pathConsumer) {
        if (shouldLoadSyncedMods) {
            origin.scanFile(modFile, pathConsumer);
        }
    }

    @Override
    public void initArguments(Map<String, ?> arguments) {
        var modDir = gameDirectory.resolve(configManager.getConfig().directory);
        if (!modDir.toFile().exists()) {
            modDir.toFile().mkdirs();
        }

        origin = Launcher.INSTANCE.environment()
                .getProperty(Environment.Keys.MODDIRECTORYFACTORY.get())
                .orElseThrow(() -> new RuntimeException("There is no Mod Directory Factory!"))
                .build(modDir, Constants.NAME + "Mods Directory");
    }

    @Override
    public boolean isValid(IModFile modFile) {
        return true;
//        return !Arrays.stream(modManifest.mods).noneMatch(m -> {
//            try {
//                return m.path.equals(modFile.getFileName())
//                        && m.checksum.equalsIgnoreCase(
//                        DigestUtils.sha256Hex(FileUtils.readFileToByteArray(modFile.getFilePath().toFile())));
//            } catch (IOException ignored) {
//                return false;
//            }
//        });
    }
}
