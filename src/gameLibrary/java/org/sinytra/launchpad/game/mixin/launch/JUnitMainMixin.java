/*
 * Copyright (c) 2026 Sinytra
 * SPDX-License-Identifier: GPL-3.0-only WITH Classpath-exception-2.0
 */

package org.sinytra.launchpad.game.mixin.launch;

import net.neoforged.neoforge.junit.JUnitMain;
import org.sinytra.launchpad.game.EntrypointRunner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(JUnitMain.class)
public class JUnitMainMixin {
    @Inject(method = "bootstrap", at = @At(value = "INVOKE", target = "Lnet/neoforged/neoforge/server/loading/ServerModLoader;load(Z)V"))
    private void earlyInit(CallbackInfo ci) {
        EntrypointRunner.invokeEntrypoints();
    }
}
