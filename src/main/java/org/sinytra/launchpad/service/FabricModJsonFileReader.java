/*
 * Copyright (c) 2026 Sinytra
 * SPDX-License-Identifier: GPL-3.0-only WITH Classpath-exception-2.0
 */

package org.sinytra.launchpad.service;

import com.mojang.logging.LogUtils;
import net.neoforged.fml.jarcontents.JarContents;
import net.neoforged.fml.loading.LogMarkers;
import net.neoforged.neoforgespi.locating.IModFile;
import net.neoforged.neoforgespi.locating.IModFileReader;
import net.neoforged.neoforgespi.locating.ModFileDiscoveryAttributes;
import org.jetbrains.annotations.Nullable;
import org.sinytra.launchpad.impl.FabricModFactoryImpl;
import org.sinytra.launchpad.impl.LaunchpadImpl;
import org.slf4j.Logger;

public class FabricModJsonFileReader implements IModFileReader {
    private static final Logger LOGGER = LogUtils.getLogger();

    @Override
    @Nullable
    public IModFile read(JarContents jar, ModFileDiscoveryAttributes attributes) {
        IModFile file = FabricModFactoryImpl.createModFile(jar, attributes.withReader(this), null);
        if (file != null) {
            LOGGER.debug(LogMarkers.SCAN, "Found {} mod: {}", LaunchpadImpl.FMJ, jar.getPrimaryPath());
        }
        return file;
    }

    @Override
    public int getPriority() {
        // Run before neoforge.mods.toml reader
        return 200;
    }
}
