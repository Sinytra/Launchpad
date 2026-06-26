/*
 * Copyright (c) 2026 Sinytra
 * SPDX-License-Identifier: GPL-3.0-only WITH Classpath-exception-2.0
 */

package org.sinytra.launchpad.game.mixin.launch;

import net.neoforged.neoforge.data.loading.DatagenModLoader;
import org.sinytra.launchpad.game.EntrypointRunner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = DatagenModLoader.class, remap = false)
public abstract class DatagenModLoaderMixin {
    @Inject(method = "begin", at = @At(value = "INVOKE", target = "Lnet/neoforged/neoforge/data/loading/DatagenModLoader;begin(Ljava/lang/Runnable;Z)V"))
    private static void earlyInit(CallbackInfo ci) {
        EntrypointRunner.invokeEntrypoints();
    }
}
