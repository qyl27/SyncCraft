package al.yn.synccraft;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.forgespi.locating.IModLocator;

import java.util.ServiceLoader;

// Fixme: Not loading.
@Mod("synccraft")
public class SyncCraft {
    public SyncCraft() {
        ServiceLoader.load(IModLocator.class).stream().forEach(l -> {
            System.out.println(l.get().name());
        });
    }
}
