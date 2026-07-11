/*
 * Copyright (c) 2026 Sinytra
 * SPDX-License-Identifier: GPL-3.0-only WITH Classpath-exception-2.0
 */

package org.sinytra.launchpad.game.mixin.registries;

import net.minecraft.core.Registry;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.callback.ClearCallback;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.neoforged.neoforge.registries.NeoForgeRegistryCallbacks$BlockCallbacks")
public class BlockCallbacksMixin {
    // Clear BLOCKSTATE_TO_ID_MAP to reset the incremental ID counter
    // Not doing so will create an inconsistency between server and client registries
    // Both will count each block twice during setup, but the client resets its counter when applying the server snapshot
    // So the server will end up sending an ID like 57797 to the client which only knows IDs up to 46893
    @Inject(method = "onBake", at = @At(value = "INVOKE", target = "Ljava/util/Set;clear()V"))
    private void clearIdMapBeforeBake(Registry<Block> registry, CallbackInfo ci) {
        // Clear BLOCKSTATE_TO_ID_MAP
        ((ClearCallback<Block>) this).onClear(registry, false);
    }
}
