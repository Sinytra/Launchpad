package org.sinytra.launchpad.game.mixin.registries;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.core.Registry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceKey;
import org.sinytra.launchpad.impl.LaunchpadImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ByteBufCodecs.class)
public interface ByteBufCodecsMixin {

    @ModifyExpressionValue(
        method = "getSyncableRegistryOrThrow",
        at = @At(
            value = "INVOKE",
            target = "Lnet/neoforged/neoforge/registries/RegistryManager;isNonSyncedBuiltInRegistry(Lnet/minecraft/core/Registry;)Z"
        )
    )
    private static boolean bypassSyncCheck(boolean original, RegistryFriendlyByteBuf buffer, ResourceKey<? extends Registry<?>> registryKey) {
        ModContainer source = FabricLoader.getInstance().getModContainer(registryKey.identifier().getNamespace()).orElse(null);
        if (source != null && source.getMetadata().containsCustomValue(LaunchpadImpl.LAUNCHPAD_ACTIVE)) {
            return false;
        }
        return original;
    }
}
