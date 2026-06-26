/*
 * Copyright (c) 2026 Sinytra
 * SPDX-License-Identifier: GPL-3.0-only WITH Classpath-exception-2.0
 */

package org.sinytra.launchpad.game;

import com.mojang.logging.LogUtils;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import net.neoforged.fml.loading.progress.ProgressMeter;
import net.neoforged.fml.loading.progress.StartupNotificationManager;
import org.objectweb.asm.tree.ClassNode;
import org.sinytra.launchpad.impl.LaunchpadImpl;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class LaunchpadMixinPlugin implements IMixinConfigPlugin {
    private static final Logger LOGGER = LogUtils.getLogger();

    static {
        invokePreLaunchEntrypoints();
    }

    private static void invokePreLaunchEntrypoints() {
        if (LaunchpadImpl.hasLoadingError()) {
            LOGGER.error("Skipping fabric prelaunch entrypoint invocation due to previous error");
            return;
        }

        LOGGER.debug("Invoking Fabric preLaunch entrypoint");

        ProgressMeter progress = StartupNotificationManager.prependProgressBar("[Launchpad] PreLaunch", 0);

        ScopedValue.where(LaunchpadImpl.LOADING, true).run(() -> {
            try {
                FabricLoader.getInstance().invokeEntrypoints("preLaunch", PreLaunchEntrypoint.class, PreLaunchEntrypoint::onPreLaunch);
            } catch (Throwable t) {
                LOGGER.error("Failed to invoke prelaunch entrypoint", t);
                LaunchpadImpl.addLoadingException(t, "Failed to invoke prelaunch entrypoint");
            }
        });

        progress.complete();
    }

    // @formatter:off
    @Override public void onLoad(String mixinPackage) {}
    @Override public String getRefMapperConfig() {return null;}
    @Override public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return true;
    }
    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}
    @Override public List<String> getMixins() {return List.of();}
    @Override public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
    @Override public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
    // @formatter:on
}
