/*
 * Copyright (c) 2026 Sinytra
 * SPDX-License-Identifier: GPL-3.0-only WITH Classpath-exception-2.0
 */

package org.sinytra.launchpad.testmod;

import com.mojang.logging.LogUtils;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import org.slf4j.Logger;

public class PrelaunchMain implements PreLaunchEntrypoint {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean initialized;

    public static boolean isInitialized() {
        return initialized;
    }

    @Override
    public void onPreLaunch() {
        LOGGER.info("Called PrelaunchMain#onPreLaunch!");
        initialized = true;
    }
}
