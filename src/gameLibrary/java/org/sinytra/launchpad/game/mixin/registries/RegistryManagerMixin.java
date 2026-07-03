/*
 * Copyright (c) 2026 Sinytra
 * SPDX-License-Identifier: GPL-3.0-only WITH Classpath-exception-2.0
 */

package org.sinytra.launchpad.game.mixin.registries;

import net.neoforged.neoforge.registries.RegistryManager;
import org.sinytra.launchpad.api.Launchpad;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RegistryManager.class)
public class RegistryManagerMixin {
    @Inject(method = "trackModdedRegistry", at = @At("HEAD"), cancellable = true)
    private static void allowRegistration(CallbackInfo ci) {
        if (Launchpad.isLoading()) {
            ci.cancel();
        }
    }
}
