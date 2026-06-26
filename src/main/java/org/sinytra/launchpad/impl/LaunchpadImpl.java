/*
 * Copyright (c) 2026 Sinytra
 * SPDX-License-Identifier: GPL-3.0-only WITH Classpath-exception-2.0
 */

package org.sinytra.launchpad.impl;

import net.neoforged.fml.ModLoadingIssue;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.LoadingModList;

import java.util.Arrays;
import java.util.Objects;

public class LaunchpadImpl {
    public static final String NAMESPACE = "launchpad";
    // Internal metadata properties
    public static final String LAUNCHPAD_ACTIVE = "launchpad:active";
    // Provided by forgified-fabric-loader
    public static final String FABRIC_METADATA = "fabric:metadata";

    public static final ScopedValue<Boolean> LOADING = ScopedValue.newInstance();

    public static boolean hasLoadingError() {
        LoadingModList modList = FMLLoader.getCurrent().getLoadingModList();
        return modList != null && modList.hasErrors();
    }

    public static void addLoadingException(Throwable original, String message) {
        ModLoadingIssue issue = new ModLoadingIssue(
            ModLoadingIssue.Severity.ERROR,
            "§e[Launchpad]§r {0}\n§c{1}§7: {2}§r",
            Arrays.asList(message, original.getClass().getName(), original.getMessage()),
            original,
            null, null, null
        );

        Objects.requireNonNull(FMLLoader.getCurrent().getLoadingModList(), "Missing loading mod list. Too early?")
            .getModLoadingIssues()
            .add(issue);
    }
}
