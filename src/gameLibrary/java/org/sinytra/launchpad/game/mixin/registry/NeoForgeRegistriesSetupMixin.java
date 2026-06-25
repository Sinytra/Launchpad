package org.sinytra.launchpad.game.mixin.registry;

import net.neoforged.neoforge.registries.ModifyRegistriesEvent;
import net.neoforged.neoforge.registries.NeoForgeRegistriesSetup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NeoForgeRegistriesSetup.class)
public abstract class NeoForgeRegistriesSetupMixin {
    @Unique
    private static boolean launchpad$modified;

    @Inject(method = "modifyRegistries", at = @At("HEAD"), cancellable = true)
    private static void setup(ModifyRegistriesEvent event, CallbackInfo ci) {
        if (launchpad$modified) {
            ci.cancel();
            return;
        }

        launchpad$modified = true;
    }
}
