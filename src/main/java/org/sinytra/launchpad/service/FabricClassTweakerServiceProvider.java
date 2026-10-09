/*
 * Copyright (c) 2026 Sinytra
 * SPDX-License-Identifier: GPL-3.0-only WITH Classpath-exception-2.0
 */

package org.sinytra.launchpad.service;

import com.mojang.logging.LogUtils;
import net.fabricmc.classtweaker.api.ClassTweaker;
import net.fabricmc.classtweaker.api.ClassTweakerReader;
import net.fabricmc.loader.impl.metadata.LoaderModMetadata;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.LogMarkers;
import net.neoforged.fml.loading.moddiscovery.ModFileInfo;
import net.neoforged.neoforgespi.locating.IModFile;
import net.neoforged.neoforgespi.transformation.ClassProcessorProvider;
import org.sinytra.launchpad.impl.EnvironmentSetup;
import org.sinytra.launchpad.impl.LaunchpadImpl;
import org.sinytra.launchpad.impl.ClassTweakerProcessor;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Objects;

public class FabricClassTweakerServiceProvider implements ClassProcessorProvider {
    private static final Logger LOGGER = LogUtils.getLogger();

    public FabricClassTweakerServiceProvider() {
        // The mixin service has been initialized at this point
        EnvironmentSetup.enableEnumExtensions();
    }

    @Override
    public void createProcessors(Context context, Collector collector) {
        ClassTweaker tweaker = ClassTweaker.newInstance();
        ClassTweakerReader reader = ClassTweakerReader.create(tweaker);

        for (ModFileInfo modFileInfo : FMLLoader.getCurrent().getLoadingModList().getModFiles()) {
            if (modFileInfo.getFileProperties().get(LaunchpadImpl.LAUNCHPAD_ACTIVE) != Boolean.TRUE) {
                continue;
            }

            LoaderModMetadata metadata = (LoaderModMetadata) Objects.requireNonNull(
                modFileInfo.getFileProperties().get(LaunchpadImpl.FABRIC_METADATA),
                "Missing launchpad fabric mod metadata"
            );
            String ctPath = metadata.getClassTweaker();
            if (ctPath == null) {
                continue;
            }

            IModFile modFile = modFileInfo.getFile();
            LOGGER.debug("Adding Class Tweaker {} from {}", ctPath, modFile);
            try (InputStream in = modFile.getContents().openFile(ctPath)) {
                if (in == null) {
                    LOGGER.error(LogMarkers.LOADING, "Class Tweaker file {} provided by {} does not exist!", ctPath, modFile);
                    continue;
                }

                reader.read(new BufferedReader(new InputStreamReader(in)), "official");
            } catch (IOException e) {
                throw new RuntimeException("Failed to read Class Tweaker at " + ctPath + " from " + modFile, e);
            }
        }

        if (!tweaker.getTargets().isEmpty()) {
            collector.add(new ClassTweakerProcessor(tweaker));
        }
    }
}
