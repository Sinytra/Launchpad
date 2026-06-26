package org.sinytra.launchpad.service;

import com.mojang.logging.LogUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.jarcontents.JarContents;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.LogMarkers;
import net.neoforged.fml.loading.moddiscovery.ModJarMetadata;
import net.neoforged.fml.loading.moddiscovery.readers.JarModsDotTomlModFileReader;
import net.neoforged.neoforgespi.locating.IModFile;
import net.neoforged.neoforgespi.locating.IModFile.Type;
import net.neoforged.neoforgespi.locating.IModFileReader;
import net.neoforged.neoforgespi.locating.ModFileDiscoveryAttributes;
import org.jetbrains.annotations.Nullable;
import org.sinytra.launchpad.impl.FabricModMetadata;
import org.slf4j.Logger;

import java.util.jar.Attributes;

public class FabricModJsonFileReader implements IModFileReader {
    public static final String FMJ = "fabric.mod.json";
    private static final Logger LOGGER = LogUtils.getLogger();

    @Override
    @Nullable
    public IModFile read(JarContents jar, ModFileDiscoveryAttributes attributes) {
        return createModFile(jar, attributes.withReader(this));
    }

    @Override
    public int getPriority() {
        // Run after neoforge.mods.toml reader
        return -500;
    }

    @Nullable
    public static IModFile createModFile(JarContents contents, ModFileDiscoveryAttributes discoveryAttributes) {
        if (!contents.containsFile(FMJ)) {
            return null;
        }

        FabricModMetadata metadata = FabricModMetadata.parse(contents);
        if (metadata == null) {
            return null;
        }

        LOGGER.debug(LogMarkers.SCAN, "Found {} mod: {}", FMJ, contents.getPrimaryPath());

        Dist dist = FMLLoader.getCurrent().getDist();
        Attributes manifest = contents.getManifest().getMainAttributes();

        if (metadata.isGenerated(manifest)) {
            return IModFile.create(contents, JarModsDotTomlModFileReader::manifestParser, IModFile.Type.GAMELIBRARY, ModFileDiscoveryAttributes.DEFAULT);
        }

        ModJarMetadata mjm = new ModJarMetadata();
        IModFile modFile = IModFile.create(contents, mjm, metadata.createNeoMetadataFactory(dist), Type.MOD, discoveryAttributes);
        mjm.setModFile(modFile);

        return modFile;
    }
}
