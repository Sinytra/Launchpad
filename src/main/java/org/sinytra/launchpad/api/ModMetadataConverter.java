package org.sinytra.launchpad.api;

import net.fabricmc.loader.impl.metadata.LoaderModMetadata;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforgespi.language.IModFileInfo;
import net.neoforged.neoforgespi.locating.IModFile;
import org.sinytra.launchpad.impl.MetadataConverter;

public interface ModMetadataConverter {
    static IModFileInfo createNeoModMetadata(LoaderModMetadata metadata, IModFile modFile, Dist dist) {
        return MetadataConverter.createNeoMetadata(metadata, modFile, dist);
    }
}
