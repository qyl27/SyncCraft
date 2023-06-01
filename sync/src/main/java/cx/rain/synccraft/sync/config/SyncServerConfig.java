package cx.rain.synccraft.sync.config;

public class SyncServerConfig implements ISyncConfig {
    public int configVersion = 1;

    // Todo: qyl27: Provider mode is not implemented.
    // There is two working mode in server.
    // Provider: Server is the host of mod file, clients will download mods from server.
    // Consumer: Server is a consumer like client, mods hosted by dedicated web file server. (Or OSS, CDN, etc.)
    public String workingMode = "Consumer";
    public int providerPort = 35196;
}
