package org.sinytra.launchpad.game.mixin.launch;

import net.minecraft.client.Minecraft;
import net.minecraft.client.main.GameConfig;
import org.sinytra.launchpad.game.EntrypointRunner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Minecraft.class, priority = 3000)
public abstract class MinecraftMixin {
    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Ljava/lang/Thread;currentThread()Ljava/lang/Thread;"), remap = false)
    private void earlyInit(GameConfig gameConfig, CallbackInfo ci) {
        EntrypointRunner.invokeEntrypoints();
    }
}
