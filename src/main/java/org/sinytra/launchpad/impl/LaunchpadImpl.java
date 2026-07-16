/*
 * Copyright (c) 2026 Sinytra
 * SPDX-License-Identifier: GPL-3.0-only WITH Classpath-exception-2.0
 */

package org.sinytra.launchpad.impl;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.neoforged.fml.ModLoadingException;
import net.neoforged.fml.ModLoadingIssue;

import java.util.Arrays;

public class LaunchpadImpl {
    public static final String NAMESPACE = "launchpad";
    // Fabric metadata file
    public static final String FMJ = "fabric.mod.json";
    // Internal metadata properties
    public static final String LAUNCHPAD_ACTIVE = "launchpad:active";
    // Provided by forgified-fabric-loader
    public static final String FABRIC_METADATA = "fabric:metadata";
    // Provided by forgified-fabric-api
    public static final String POLYFILL_FLUID_TYPES = "sinytra:polyfill_fluid_types";

    public static final ScopedValue<Boolean> LOADING = ScopedValue.newInstance();

    public static void throwLoadingException(Throwable original, String message) {
        ModLoadingIssue issue = new ModLoadingIssue(
            ModLoadingIssue.Severity.ERROR,
            "§e[Launchpad]§r {0}\n§c{1}§7: {2}§r",
            Arrays.asList(message, original.getClass().getName(), original.getMessage()),
            original,
            null, null, null
        );
        throw new ModLoadingException(issue);
    }

    public static boolean isLaunchpadMod(String modId) {
        ModContainer source = FabricLoader.getInstance().getModContainer(modId).orElse(null);
        return source != null && source.getMetadata().containsCustomValue(LaunchpadImpl.LAUNCHPAD_ACTIVE);
    }
}
