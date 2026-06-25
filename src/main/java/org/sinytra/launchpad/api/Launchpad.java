package org.sinytra.launchpad.api;

import org.sinytra.launchpad.impl.LaunchpadImpl;

public final class Launchpad {

    public static boolean isLoading() {
        return LaunchpadImpl.LOADING.orElse(false);
    }
    
    private Launchpad() {
    }
}
