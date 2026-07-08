/*
 * Copyright (c) 2026 Sinytra
 * SPDX-License-Identifier: GPL-3.0-only WITH Classpath-exception-2.0
 */

package org.sinytra.launchpad.game.mixin.launch;

import net.neoforged.neoforge.server.loading.ServerModLoader;
import org.sinytra.launchpad.game.EntrypointRunner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerModLoader.class)
public class ServerModLoaderMixin {
    @Inject(method = "load", at = @At(value = "INVOKE", target = "Lnet/neoforged/neoforge/server/loading/ServerModLoader;begin(Ljava/lang/Runnable;Z)V", shift = Shift.AFTER))
    private static void initFabricMods(CallbackInfo ci) {
        EntrypointRunner.invokeEntrypoints();
    }
}
