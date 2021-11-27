package al.yn.synccraft.service;

import cpw.mods.jarhandling.impl.Jar;
import cpw.mods.jarhandling.impl.SimpleJarMetadata;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.function.BiPredicate;
import java.util.jar.Manifest;

public class SyncCraftJar extends Jar {
    public SyncCraftJar(Path... paths) {
        super(Manifest::new, jar -> new SimpleJarMetadata(jar.name(), null, jar.getPackages(), new ArrayList<>()), (s1, s2) -> true, paths);
    }
}
