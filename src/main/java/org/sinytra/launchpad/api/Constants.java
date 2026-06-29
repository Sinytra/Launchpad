/*
 * Copyright (c) 2026 Sinytra
 * SPDX-License-Identifier: GPL-3.0-only WITH Classpath-exception-2.0
 */

package org.sinytra.launchpad.api;

import net.neoforged.neoforgespi.transformation.ProcessorName;
import org.sinytra.launchpad.impl.LaunchpadImpl;

public final class Constants {
    // FMJ custom metadata properties
    public static final String ENABLE_LAUNCHPAD = "launchpad:compatible";
    public static final String OVERRIDES = "launchpad:overrides";
    
    // neoforge.mods.toml properties
    public static final String PLACEHOLDER = "launchpad:placeholder";

    // Services
    public static final ProcessorName AT_PROCESSOR = new ProcessorName(LaunchpadImpl.NAMESPACE, "access_transformer");

    private Constants() {
    }
}
