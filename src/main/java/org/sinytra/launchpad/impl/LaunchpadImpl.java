/*
 * Copyright (c) 2026 Sinytra
 * SPDX-License-Identifier: GPL-3.0-only WITH Classpath-exception-2.0
 */

package org.sinytra.launchpad.impl;

import net.neoforged.fml.ModLoadingException;
import net.neoforged.fml.ModLoadingIssue;

import java.util.Arrays;

public class LaunchpadImpl {
    public static final String NAMESPACE = "launchpad";
    // Internal metadata properties
    public static final String LAUNCHPAD_ACTIVE = "launchpad:active";
    // Provided by forgified-fabric-loader
    public static final String FABRIC_METADATA = "fabric:metadata";

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
}
