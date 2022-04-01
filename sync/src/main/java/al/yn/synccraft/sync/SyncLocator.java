package al.yn.synccraft.sync;

import al.yn.synccraft.sync.data.Config;
import al.yn.synccraft.sync.data.ModManifest;
import com.google.gson.Gson;
import cpw.mods.modlauncher.Launcher;
import cpw.mods.modlauncher.api.IEnvironment;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.moddiscovery.ModJarMetadata;
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
import java.util.*;
import java.util.function.Consumer;

public final class SyncLocator implements IModLocator {
    private static final Logger LOG = LogManager.getLogger("SyncCraft");
    private static final Gson GSON = new Gson();

    private Path gameDirectory;
    private File configFile;
    private Config config;

    private Path modDirectory;

    private ModManifest modManifest;

    private IModLocator origin;

    private boolean shouldLoadSyncedMods = false;

    public SyncLocator() throws Exception {
        LOG.info("Initializing SyncCraft v1.1.0.");

        var dist = Launcher.INSTANCE.environment().getProperty(Environment.Keys.DIST.get());
        assert dist != null && dist.isPresent();
        if (dist.get() == Dist.CLIENT) {
            onClient();
        } else {
            LOG.error("Not supported server yet.");
            throw new Exception("Not supported server yet.");
        }
    }

    private void onClient() throws Exception {
        shouldLoadSyncedMods = true;

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
        try (var stream = new URL(config.server + "/mods_manifest.json").openStream()) {
            var manifestString = IOUtils.toString(stream, StandardCharsets.UTF_8);
            modManifest = GSON.fromJson(manifestString, ModManifest.class);
        } catch (IOException ex) {
            LOG.error("Cannot fetch mods manifest file. Load normally.");
            // No delete mod directory, but not loading.
//            cleanUp(modDirectory);
            shouldLoadSyncedMods = false;
            return;
        }

        if (!config.serverName.equals(modManifest.name)) {
            LOG.error("Mismatched server name, so we will not work for you.");
//            cleanUp(modDirectory);
            shouldLoadSyncedMods = false;
            return;
        }

        syncMods();
        syncConfigs();

        LOG.info("All the mods and configs are update to date.");
    }

    private void syncConfigs() throws Exception {
        var configsDirectory = gameDirectory.resolve("config").toFile();

        // Check mod lists.
        LOG.info("Checking configs.");
        var shouldDownload = checkEntries(configsDirectory,
                Arrays.stream(modManifest.configs).toList(), false);

        LOG.info("Ready for sync configs.");
        for (var conf : shouldDownload) {
            // Download mods.
            FileUtils.copyURLToFile(new URL(config.server + "/" + conf.path.replace("\\", "/")),
                    new File(gameDirectory.toFile(), conf.path));
            LOG.info("Downloaded config " + conf.path + " from remote server.");
        }
    }

    private void syncMods() throws Exception {
        // Check mod lists.
        LOG.info("Checking mods.");
        var shouldDownload = checkEntries(gameDirectory.resolve("mods").toFile(),
                Arrays.stream(modManifest.mods).toList(), true);

        LOG.info("Ready for sync mods.");
        for (var mod : shouldDownload) {
            // Download mods.
            FileUtils.copyURLToFile(new URL(config.server + "/mods/" + mod.path.replace("\\", "/")),
                    new File(modDirectory.toFile(), mod.path));
            LOG.info("Downloaded mod " + mod.path + " from remote server.");
        }
    }

    private List<ModManifest.Entry> checkEntries(File dir, List<ModManifest.Entry> entries, boolean isMod) {
        if ((!isMod) && (!config.syncConfig)) {
            return new ArrayList<>();
        }

        var filesLocal = enumerateFiles(dir, !isMod, new HashSet<>());

        var shouldDownload = new ArrayList<>(entries);

        for (var file : filesLocal) {
            // Check files one by one.
            if (entries.stream().noneMatch(m -> {
                try {
                    if (m.path.equals(file.getName())
                            && m.checksum.equalsIgnoreCase(
                            DigestUtils.sha256Hex(FileUtils.readFileToByteArray(file)))) {
                        shouldDownload.remove(m);
                        return true;
                    } else {
                        return false;
                    }
                } catch (IOException ignored) {
                    return false;
                }
            })) {
                // Check if not weak sync nor config.
                if (!config.weakSync && isMod) {
                    // Delete mismatched files.
                    var result = file.delete();
                    if (!result) {
                        LOG.error("Cannot delete mismatched file " + file.getName() + " .");
                        shouldLoadSyncedMods = false;
                    }
                }
            }
        }

        return shouldDownload;
    }

    private void cleanUp(Path path) throws Exception {
        Files.deleteIfExists(path);
    }

    private Set<File> enumerateFiles(File file, boolean includeChildDir, Set<File> fileSet) {
        if (file.isDirectory()) {
            if (includeChildDir) {
                for (var child : file.listFiles()) {
                    fileSet.addAll(enumerateFiles(child, includeChildDir, fileSet));
                }
            }

            fileSet.addAll(Arrays.stream(file.listFiles()).toList());
        } else {
            fileSet.add(file);
        }

        return fileSet;
    }

    @Override
    public List<IModFile> scanMods() {
//        var mods = new ArrayList<IModFile>();
//
//        Path path = null;
//        try {
//            path = ((UnionFileSystem) Paths.get(this.getClass().getProtectionDomain()
//                    .getCodeSource().getLocation().toURI()).getFileSystem()).getPrimaryPath();
//        } catch (URISyntaxException ex) {
//            ex.printStackTrace();
//        }
//
//        mods.add(ModFileFactory.FACTORY.build(SecureJar.from(path), this, ))

        if (shouldLoadSyncedMods) {
            return origin.scanMods();
        } else {
            return new ArrayList<>();
        }
    }

    @Override
    public String name() {
        return "SyncCraft";
    }

    @Override
    public void scanFile(IModFile modFile, Consumer<Path> pathConsumer) {
        if (shouldLoadSyncedMods) {
            origin.scanFile(modFile, pathConsumer);
        }
    }

    @Override
    public void initArguments(Map<String, ?> arguments) {
        origin = Launcher.INSTANCE.environment()
                .getProperty(Environment.Keys.MODDIRECTORYFACTORY.get())
                .orElseThrow(() -> new RuntimeException("There is no Mod Directory Factory!"))
                .build(modDirectory, "SyncCraft Mods Directory");
    }

    @Override
    public boolean isValid(IModFile modFile) {
        return !Arrays.stream(modManifest.mods).noneMatch(m -> {
            try {
                return m.path.equals(modFile.getFileName())
                        && m.checksum.equalsIgnoreCase(
                        DigestUtils.sha256Hex(FileUtils.readFileToByteArray(modFile.getFilePath().toFile())));
            } catch (IOException ignored) {
                return false;
            }
        });
    }
}
