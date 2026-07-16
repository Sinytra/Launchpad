/*
 * Copyright (c) 2026 Sinytra
 * SPDX-License-Identifier: GPL-3.0-only WITH Classpath-exception-2.0
 */

package org.sinytra.launchpad.impl;

import com.mojang.logging.LogUtils;
import net.neoforged.fml.loading.mixin.FMLMixinService;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.FabricUtil;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.transformer.ClassInfo;
import org.spongepowered.asm.mixin.transformer.throwables.InvalidMixinException;
import org.spongepowered.asm.service.IFeatureValidator;
import org.spongepowered.asm.service.IMixinService;
import org.spongepowered.asm.service.MixinService;
import sun.misc.Unsafe;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;

@SuppressWarnings("removal")
public class EnvironmentSetup {
    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * Allow mods to extend arbitrary enums using Mixin's enum extensions, even if the enum implements IExtensibleEnum.
     * Use at your own risk.
     */
    public static void enableEnumExtensions() {
        try {
            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            Unsafe unsafe = (Unsafe) theUnsafe.get(null);

            Field hackfield = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
            MethodHandles.Lookup trustedLookup = (MethodHandles.Lookup) unsafe.getObject(unsafe.staticFieldBase(hackfield), unsafe.staticFieldOffset(hackfield));

            VarHandle featureValidatorHandle = trustedLookup.findVarHandle(FMLMixinService.class, "featureValidator", IFeatureValidator.class);
            IMixinService service = MixinService.getService();
            IFeatureValidator original = (IFeatureValidator) featureValidatorHandle.get(service);
            featureValidatorHandle.set(service, new LaunchpadFeatureValidator(original));
        } catch (Throwable t) {
            LOGGER.error("Error enabling Mixin enum extension support", t);
        }
    }

    private static class LaunchpadFeatureValidator implements IFeatureValidator {
        private final IFeatureValidator wrapped;

        public LaunchpadFeatureValidator(IFeatureValidator wrapped) {
            this.wrapped = wrapped;
        }

        @Override
        public void validateEnumExtension(IMixinInfo mixin, ClassInfo targetClass) throws InvalidMixinException {
            String modId = mixin.getConfig().getDecoration(FabricUtil.KEY_MOD_ID);
            if (LaunchpadImpl.isLaunchpadMod(modId)) {
                return;
            }
            this.wrapped.validateEnumExtension(mixin, targetClass);
        }
    }
}
