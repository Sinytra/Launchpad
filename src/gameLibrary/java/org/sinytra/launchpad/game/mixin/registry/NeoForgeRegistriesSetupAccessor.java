/*
 * Copyright (c) 2026 Sinytra
 * SPDX-License-Identifier: GPL-3.0-only WITH Classpath-exception-2.0
 */

package org.sinytra.launchpad.game.mixin.registry;

import net.neoforged.neoforge.registries.ModifyRegistriesEvent;
import net.neoforged.neoforge.registries.NeoForgeRegistriesSetup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(NeoForgeRegistriesSetup.class)
public interface NeoForgeRegistriesSetupAccessor {
    @Invoker
    static void invokeModifyRegistries(ModifyRegistriesEvent event) {
        throw new UnsupportedOperationException();
    }
}
