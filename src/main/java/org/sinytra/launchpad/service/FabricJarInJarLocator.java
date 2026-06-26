/*
 * Copyright (c) 2026 Sinytra
 * SPDX-License-Identifier: GPL-3.0-only WITH Classpath-exception-2.0
 */

package org.sinytra.launchpad.service;

import com.mojang.logging.LogUtils;
import net.fabricmc.loader.impl.metadata.LoaderModMetadata;
import net.fabricmc.loader.impl.metadata.NestedJarEntry;
import net.neoforged.fml.loading.moddiscovery.ModFile;
import net.neoforged.neoforgespi.locating.IDependencyLocator;
import net.neoforged.neoforgespi.locating.IDiscoveryPipeline;
import net.neoforged.neoforgespi.locating.IModFile;
import org.sinytra.launchpad.impl.JarInJarHelpers;
import org.sinytra.launchpad.impl.LaunchpadImpl;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class FabricJarInJarLocator implements IDependencyLocator {
    private static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public void scanMods(List<IModFile> loadedMods, IDiscoveryPipeline pipeline) {
        List<IModFile> dependenciesToLoad = new ArrayList<>();

        for (IModFile modFile : loadedMods) {
            recursivelyDiscoverNestedJars(modFile, pipeline, dependenciesToLoad);
        }

        if (dependenciesToLoad.isEmpty()) {
            LOGGER.info("No nested jars to load found. Skipping!");
        } else {
            LOGGER.info("Found {} nested jars, adding them to mods collection", dependenciesToLoad.size());
            for (var modFile : dependenciesToLoad) {
                if (!pipeline.addModFile(modFile)) {
                    ((ModFile) modFile).close();
                }
            }
        }
    }

    @Override
    public int getPriority() {
        // Run after NeoForge JiJ locator
        return -500;
    }

    private static void recursivelyDiscoverNestedJars(IModFile parent, IDiscoveryPipeline pipeline, List<? super IModFile> output) {
        LoaderModMetadata metadata = (LoaderModMetadata) parent.getModFileInfo().getFileProperties().get(LaunchpadImpl.FABRIC_METADATA);
        if (metadata == null) {
            return;
        }

        for (NestedJarEntry entry : metadata.getJars()) {
            IModFile nestedFile = JarInJarHelpers.loadModFileFrom(parent, entry.getFile(), pipeline);
            output.add(nestedFile);

            recursivelyDiscoverNestedJars(nestedFile, pipeline, output);
        }
    }
}
