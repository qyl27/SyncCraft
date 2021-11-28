package al.yn.synccraft.data;

public class ModManifest {
    public String name;
    public ModEntry[] mods;

    public static class ModEntry {
        public String name;
        public String checksum;
    }
}
