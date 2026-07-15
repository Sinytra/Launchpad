/*
 * Copyright (c) 2026 Sinytra
 * SPDX-License-Identifier: GPL-3.0-only WITH Classpath-exception-2.0
 */

package org.sinytra.launchpad.service;

import net.neoforged.fml.util.ClasspathResourceUtils;
import net.neoforged.neoforgespi.ILaunchContext;
import net.neoforged.neoforgespi.locating.IDiscoveryPipeline;
import net.neoforged.neoforgespi.locating.IModFileCandidateLocator;
import net.neoforged.neoforgespi.locating.IncompatibleFileReporting;
import net.neoforged.neoforgespi.locating.ModFileDiscoveryAttributes;
import org.sinytra.launchpad.impl.LaunchpadImpl;

import java.nio.file.Files;

public class InDevFabricJarLocator implements IModFileCandidateLocator {
    @Override
    public void findCandidates(ILaunchContext context, IDiscoveryPipeline pipeline) {
        for (var path : ClasspathResourceUtils.findFileSystemRootsOfFileOnClasspath(LaunchpadImpl.FMJ)) {
            if (Files.isRegularFile(path)) {
                pipeline.addPath(path, ModFileDiscoveryAttributes.DEFAULT, IncompatibleFileReporting.IGNORE);
            }
        }
    }

    @Override
    public int getPriority() {
        return 500;
    }

    @Override
    public String toString() {
        return "lauchpad:indevjar";
    }
}
