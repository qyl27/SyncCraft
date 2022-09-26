package cx.rain.synccraft.sync;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import cpw.mods.modlauncher.Launcher;
import cpw.mods.modlauncher.api.IEnvironment;
import cx.rain.synccraft.Constants;
import cx.rain.synccraft.sync.config.SyncConfigManager;
import cx.rain.synccraft.sync.data.ModsManifest;
import cx.rain.synccraft.sync.utility.FileHelper;
import net.minecraftforge.forgespi.Environment;
import net.minecraftforge.forgespi.locating.IModFile;
import net.minecraftforge.forgespi.locating.IModLocator;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.VersionRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class SyncLocator implements IModLocator {
    private static final Logger LOGGER = LoggerFactory.getLogger(Constants.NAME);
    private static final Gson GSON = new Gson();

    private Path gameDirectory;
    private Path syncDirectory;
    private SyncConfigManager configManager;

    private ModsManifest manifest;

    private IModLocator origin;
    private boolean shouldLoadSyncedMods = false;

    public SyncLocator() throws Exception {
        LOGGER.info("Initializing SyncCraft ver: " + Constants.VERSION);

        gameDirectory = Launcher.INSTANCE.environment()
                .getProperty(IEnvironment.Keys.GAMEDIR.get())
                .orElse(Paths.get("."));

        var dist = Launcher.INSTANCE.environment().getProperty(Environment.Keys.DIST.get());
        assert dist != null && dist.isPresent();
        if (dist.get().isClient()) {
            configManager = new SyncConfigManager(gameDirectory, false);
            onConsumer();
        } else {
            configManager = new SyncConfigManager(gameDirectory, true);
            if (configManager.hasServerConfig()) {
                if (configManager.getServerConfig().workingMode.equalsIgnoreCase(Constants.WORKING_MODE_PROVIDER)) {
                    // Todo.
                    LOGGER.error("Not implemented in server yet.");
                    throw new RuntimeException("Not implemented in server yet.");
                } else {
                    onConsumer();
                }
            } else {
                LOGGER.error("Why no server config? It should not be happen!");
                throw new RuntimeException("Why no server config? It should not be happen!");
            }
        }

        if (!shouldLoadSyncedMods) {
            LOGGER.info("Load as normal.");
        } else {
            LOGGER.info("Loading synced mods.");
        }
    }

    private void onConsumer() throws Exception {
        // Load configs.
        syncDirectory = gameDirectory.resolve(configManager.getConfig().directory);

        var url = configManager.getConfig().server;
        if (!url.endsWith("/")) {
            url += "/";
        }
        url += "mods.json";

        try {
            var manifestString = IOUtils.toString(new URL(url), StandardCharsets.UTF_8);
            manifest = GSON.fromJson(manifestString, ModsManifest.class);
        } catch (MalformedURLException ex) {
            LOGGER.warn("URL cannot be parse. Is it correct?");
            return;
        } catch (IOException ex) {
            LOGGER.warn("Cannot load mods manifest from server.");
            return;
        } catch (JsonSyntaxException ex) {
            LOGGER.warn("Cannot parse mods manifest file.");
            return;
        }

        var acceptMod = VersionRange.createFromVersionSpec(manifest.acceptClientMod);
        var modVersion = new DefaultArtifactVersion(Constants.VERSION);
        if (configManager.getConfig().configVersion != Constants.SUPPORTED_CONFIG_VERSION
                || !acceptMod.containsVersion(modVersion)) {
            LOGGER.error("Mod version is not supported, please contract with server owner.");
            return;
        }

        var acceptPackVersions = VersionRange.createFromVersionSpec(manifest.acceptClientPack);
        var packVersion = new DefaultArtifactVersion(configManager.getConfig().packVersion);
        if (manifest.manifestVersion != Constants.SUPPORTED_MANIFEST_VERSION
                || !acceptPackVersions.containsVersion(packVersion)) {
            LOGGER.error("Pack version is not supported, please contract with server owner.");
            throw new RuntimeException(manifest.notSupportedMessage);
        }

        if (!configManager.getConfig().serverName.equals(manifest.serverName)) {
            LOGGER.error("Mismatched server name, we will not sync for it.");
            return;
        }

        var cacheTimestamp = syncDirectory.resolve("timestamp.cache");

        if (cacheTimestamp.toFile().exists()) {
            var cacheTimestampStr = Files.readString(cacheTimestamp);
            if (manifest.timestamp.equals(cacheTimestampStr)) {
                LOGGER.info("Same timestamp in cache, skip sync.");
                return;
            }
        }

        doModsSync();
        doConfigsSync();

        if (!configManager.isServer()) {
            doResourcesSync();
        }

        Files.writeString(cacheTimestamp, manifest.timestamp);

        shouldLoadSyncedMods = true;
    }

    private void doModsSync() throws Exception {
        LOGGER.info("Checking mods.");

        if (manifest.mods.length == 0) {
            LOGGER.info("No mods to sync.");
            return;
        }

        var modDir = syncDirectory.resolve("mods").toFile();
        var modsToDownload = new ArrayList<ModsManifest.ModEntry>();
        modsToDownload.addAll(List.of(manifest.mods));

        if (manifest.forceMods) {
            var filesToDelete = new HashSet<File>();
            for (var modFile : FileHelper.enumerateFiles(modDir, false, new HashSet<>())) {
                var fileName = FilenameUtils.getName(modFile.getName());

                var modEntries = Arrays.stream(manifest.mods)
                        .filter(m -> m.fileName.equals(fileName)
                                && FileHelper.matchesSHA256(modFile, m.sha256))
                        .collect(Collectors.toSet());

                if (modEntries.isEmpty()) {
                    LOGGER.info("Mod file " + fileName + " is not exists in manifest file. Deleting.");
                    filesToDelete.add(modFile);
                } else {
                    if (modEntries.size() > 1) {
                        throw new RuntimeException("Illegal mods manifest file.");
                    }

                    modsToDownload.add(modEntries.stream().findFirst().orElseThrow());
                }
            }

            for (var file : filesToDelete) {
                file.delete();
            }
        }

        for (var modEntry : modsToDownload) {
            var modFile = new File(modDir, modEntry.fileName);
            if (modFile.exists()) {
                LOGGER.info("Mod file " + modEntry.fileName + " is already exists.");
                continue;
            }

            LOGGER.info("Downloading mod " + modEntry.fileName + " from remote server.");
            var url = new URL(modEntry.url);
            FileUtils.copyURLToFile(url, modFile);
            LOGGER.info("Downloaded mod to " + modFile.getName() + " from remote server.");
        }
    }

    private void doConfigsSync() throws Exception {
        Set<ModsManifest.LocalRelativeFileEntry> filesToDownload;
        if (manifest.forceConfigs) {
            filesToDownload = forceFiles(manifest.configs);
        } else {
            filesToDownload = new HashSet<>(List.of(manifest.configs));
        }

        downloadFiles(filesToDownload);
    }

    private void doResourcesSync() throws Exception {
        Set<ModsManifest.LocalRelativeFileEntry> filesToDownload;
        if (manifest.forceResources) {
            filesToDownload = forceFiles(manifest.resources);
        } else {
            filesToDownload = new HashSet<>(List.of(manifest.resources));
        }

        downloadFiles(filesToDownload);
    }

    private Set<ModsManifest.LocalRelativeFileEntry> forceFiles(ModsManifest.LocalRelativeFileEntry[] files) {
        var filesToDownload = new HashSet<ModsManifest.LocalRelativeFileEntry>();

        for (var fileEntry : files) {
            var localFile = gameDirectory.resolve(fileEntry.localRelativePath).toFile();
            var fileName = FilenameUtils.getName(localFile.getName());

            if (!FileHelper.matchesSHA256(localFile, fileEntry.sha256)) {
                LOGGER.info("File " + fileName + " is not exists in manifest file. Deleting.");
                localFile.delete();
            } else {
                filesToDownload.add(fileEntry);
            }
        }

        return filesToDownload;
    }

    private void downloadFiles(Set<ModsManifest.LocalRelativeFileEntry> files) throws Exception {
        for (var fileEntry : files) {
            var localFile = new File(fileEntry.localRelativePath);
            var fileName = FilenameUtils.getName(localFile.getName());

            if (localFile.exists()) {
                LOGGER.info("File " + fileName + " is already exists.");
                continue;
            }

            LOGGER.info("Downloading file " + fileName + " from remote server.");
            var url = new URL(fileEntry.url);
            FileUtils.copyURLToFile(url, localFile);
            LOGGER.info("Downloaded file to " + fileName + " from remote server.");
        }
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
        var modDir = syncDirectory.resolve("mods");
        if (!modDir.toFile().exists()) {
            modDir.toFile().mkdirs();
        }

        origin = Launcher.INSTANCE.environment()
                .getProperty(Environment.Keys.MODDIRECTORYFACTORY.get())
                .orElseThrow(() -> new RuntimeException("There is no Mod Directory Factory!"))
                .build(modDir, Constants.NAME + " Mods Directory");
    }

    @Override
    public boolean isValid(IModFile modFile) {
        return true;
    }
}
