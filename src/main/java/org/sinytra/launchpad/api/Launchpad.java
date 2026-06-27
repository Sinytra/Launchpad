/*
 * Copyright (c) 2026 Sinytra
 * SPDX-License-Identifier: GPL-3.0-only WITH Classpath-exception-2.0
 */

package org.sinytra.launchpad.api;

import org.sinytra.launchpad.impl.LaunchpadImpl;

public final class Launchpad {

    /**
     * Check whether a mod's entrypoint is currently being invoked.
     * 
     * @return <code>true</code> when called from within a mod's entrypoint, otherwise <code>false</code>
     */
    public static boolean isLoading() {
        return LaunchpadImpl.LOADING.orElse(false);
    }

    private Launchpad() {
    }
}
