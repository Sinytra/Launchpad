/*
 * Copyright (c) 2026 Sinytra
 * SPDX-License-Identifier: GPL-3.0-only WITH Classpath-exception-2.0
 */

package org.sinytra.launchpad.service;

import com.mojang.logging.LogUtils;
import net.fabricmc.loader.impl.metadata.LoaderModMetadata;
import net.neoforged.accesstransformer.api.AccessTransformerEngine;
import net.neoforged.fml.common.asm.AccessTransformerService;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.LogMarkers;
import net.neoforged.fml.loading.moddiscovery.ModFileInfo;
import net.neoforged.neoforgespi.locating.IModFile;
import net.neoforged.neoforgespi.transformation.ClassProcessorProvider;
import net.neoforged.neoforgespi.transformation.ProcessorName;
import org.sinytra.launchpad.api.Constants;
import org.sinytra.launchpad.impl.ClassTweakerConverter;
import org.sinytra.launchpad.impl.LaunchpadImpl;
import org.slf4j.Logger;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

public class FabricAccessTransformerServiceProvider implements ClassProcessorProvider {
    private static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public void createProcessors(Context context, Collector collector) {
        AccessTransformerEngine engine = AccessTransformerEngine.newEngine();

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
            LOGGER.debug("Adding Class Tweaker {} in {}", ctPath, modFile);
            try (InputStream in = modFile.getContents().openFile(ctPath)) {
                if (in == null) {
                    LOGGER.error(LogMarkers.LOADING, "Class Tweaker file {} provided by {} does not exist!", ctPath, modFile);
                    continue;
                }

                String fileName = Arrays.asList(ctPath.split("/")).getLast();
                String converted = ClassTweakerConverter.createAccessTransformer(new BufferedReader(new InputStreamReader(in)), fileName);

                try (InputStream atIn = new ByteArrayInputStream(converted.getBytes(StandardCharsets.UTF_8))) {
                    engine.loadAT(new InputStreamReader(atIn), ctPath);
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to load AT at " + ctPath + " from " + modFile, e);
            }
        }

        if (!engine.getTargets().isEmpty()) {
            collector.add(new Service(engine));
        }
    }

    private static class Service extends AccessTransformerService {

        public Service(AccessTransformerEngine engine) {
            super(engine);
        }

        @Override
        public ProcessorName name() {
            return Constants.AT_PROCESSOR;
        }
    }
}
