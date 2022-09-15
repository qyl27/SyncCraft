package cx.rain.synccraft.sync.data;

public class ModsManifest {
    public int manifestVersion;

    public String serverName;
    public String acceptClientMod; // For comparing client mod version.
    public String acceptClientPack; // For comparing client pack version.

    public String notSupportedMessage;

    public String timestamp;    // 10-digits timestamp, which is the generation time of the manifest file.

    public boolean forceMods;   // Usually true.
    public boolean forceConfigs;    // Usually true.
    public boolean forceResources;  // Usually true.

    public ModEntry[] mods;
    public ConfigEntry[] configs;
    public ResourceEntry[] resources;

    public abstract class FileEntry {
        public String url;
        public String sha256;
    }

    public class ModEntry extends FileEntry {
        public String fileName;

        public String modid;    // unused.
        public String version;  // unused.
    }

    public abstract class LocalRelativeFileEntry extends FileEntry {
        public String localRelativePath;
    }

    public class ConfigEntry extends LocalRelativeFileEntry {
    }

    public class ResourceEntry extends LocalRelativeFileEntry {
        public ResourceType type;   // unused.
    }

    public enum ResourceType {
        Resource,
        Shader,
    }
}
