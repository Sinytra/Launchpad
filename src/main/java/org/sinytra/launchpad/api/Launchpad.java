/*
 * Copyright (c) 2026 Sinytra
 * SPDX-License-Identifier: GPL-3.0-only WITH Classpath-exception-2.0
 */

package org.sinytra.launchpad.api;

import org.sinytra.launchpad.impl.LaunchpadImpl;

public final class Launchpad {

    public static boolean isLoading() {
        return LaunchpadImpl.LOADING.orElse(false);
    }
    
    private Launchpad() {
    }
}
