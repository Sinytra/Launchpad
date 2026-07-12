/*
 * Copyright (c) 2026 Sinytra
 * SPDX-License-Identifier: GPL-3.0-only WITH Classpath-exception-2.0
 */

package org.sinytra.launchpad.api;

import net.neoforged.fml.jarcontents.JarContents;
import net.neoforged.neoforgespi.locating.IModFile;
import net.neoforged.neoforgespi.locating.ModFileDiscoveryAttributes;
import org.jetbrains.annotations.Nullable;
import org.sinytra.launchpad.impl.FabricModFactoryImpl;

public class FabricModFactory {
    public static IModFile createModFile(JarContents contents, ModFileDiscoveryAttributes discoveryAttributes, @Nullable IModFile.Type type) {
        return FabricModFactoryImpl.createModFile(contents, discoveryAttributes, type);
    }
}
