/*
 * Copyright (c) 2026 Sinytra
 * SPDX-License-Identifier: GPL-3.0-only WITH Classpath-exception-2.0
 */

package org.sinytra.launchpad.testmod;

import net.minecraft.server.MinecraftServer;

public interface Server {
    default MinecraftServer getSelf() {
        return (MinecraftServer) this;
    }
}
