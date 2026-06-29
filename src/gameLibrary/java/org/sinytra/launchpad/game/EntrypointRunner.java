/*
 * Copyright (c) 2026 Sinytra
 * SPDX-License-Identifier: GPL-3.0-only WITH Classpath-exception-2.0
 */

package org.sinytra.launchpad.game;

import com.mojang.logging.LogUtils;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.progress.ProgressMeter;
import net.neoforged.fml.loading.progress.StartupNotificationManager;
import net.neoforged.neoforge.registries.GameData;
import org.sinytra.launchpad.impl.LaunchpadImpl;
import org.slf4j.Logger;

public class EntrypointRunner {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static void invokeEntrypoints() {
        if (LaunchpadImpl.hasLoadingError()) {
            LOGGER.error("Skipping fabric entrypoint invocation due to previous error");
            return;
        }

        LOGGER.debug("Unfreezing data");
        GameData.unfreezeData();

        ProgressMeter progress = StartupNotificationManager.prependProgressBar("[Launchpad] Loading mods", 0);

        LOGGER.debug("Invoking Fabric entrypoints");
        ScopedValue.where(LaunchpadImpl.LOADING, true).run(() -> {
            try {
                FabricLoader loader = FabricLoader.getInstance();

                loader.invokeEntrypoints("main", ModInitializer.class, ModInitializer::onInitialize);

                if (FMLEnvironment.getDist() == Dist.CLIENT) {
                    loader.invokeEntrypoints("client", ClientModInitializer.class, ClientModInitializer::onInitializeClient);
                } else {
                    loader.invokeEntrypoints("server", DedicatedServerModInitializer.class, DedicatedServerModInitializer::onInitializeServer);
                }
            } catch (Throwable t) {
                LOGGER.error("Failed to invoke mod entrypoint", t);
                LaunchpadImpl.addLoadingException(t, "Failed to invoke mod entrypoint");
            }
        });

        GameData.freezeData();

        progress.complete();
    }
}
