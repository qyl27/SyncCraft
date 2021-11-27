package al.yn.synccraft.service;

import cpw.mods.jarhandling.SecureJar;
import cpw.mods.modlauncher.api.*;
import cpw.mods.niofs.union.UnionFileSystem;
import org.apache.logging.log4j.LogManager;

import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class TransformationService implements ITransformationService {
    public TransformationService() {
        LogManager.getLogger("SC").info("Transformation service.");
        System.out.println("Transformation service.");
    }

    @Override
    public String name() {
        return "synccraft";
    }

    @Override
    public void initialize(IEnvironment environment) {

    }

    @Override
    public void onLoad(IEnvironment env, Set<String> otherServices) throws IncompatibleEnvironmentException {

    }

    @Override
    public List<ITransformer> transformers() {
        return new ArrayList<>();
    }

    @Override
    public List<Resource> completeScan(IModuleLayerManager layerManager) {
        var list = new ArrayList<Resource>();
        var jarList = new ArrayList<SecureJar>();
        try {
//            jarList.add(new SyncCraftJar(new File(this.getClass().getProtectionDomain().getCodeSource().getLocation().toURI()).toPath()));
            jarList.add(new SyncCraftJar(((UnionFileSystem) Paths.get(this.getClass().getProtectionDomain().getCodeSource().getLocation().toURI()).getFileSystem()).getPrimaryPath()));
        } catch (URISyntaxException ex) {
            ex.printStackTrace();
        }
        list.add(new Resource(IModuleLayerManager.Layer.GAME, jarList));
        return list;
    }
}
