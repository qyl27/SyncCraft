package al.yn.synccraft.sync.data;

public class ModManifest {
    public String name;
    public Entry[] mods;
    public Entry[] configs;

    public static class Entry {
        public String path;
        public String checksum;
    }
}
