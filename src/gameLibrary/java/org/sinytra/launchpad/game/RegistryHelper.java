/*
 * Copyright (c) 2026 Sinytra
 * SPDX-License-Identifier: GPL-3.0-only WITH Classpath-exception-2.0
 */

package org.sinytra.launchpad.game;

import com.mojang.logging.LogUtils;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.neoforged.fml.ModLoader;
import net.neoforged.fml.util.ObfuscationReflectionHelper;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

public class RegistryHelper {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static void postSetup() {
        // Post EntityAttributeModificationEvent
        try {
            postModifyEntityAttributes();
        } catch (Exception e) {
            LOGGER.error("Error posting EntityAttributeModificationEvent", e);
        }
    }

    private static void postModifyEntityAttributes() {
        Map<EntityType<? extends LivingEntity>, AttributeSupplier> forgeAttributes =
            ObfuscationReflectionHelper.getPrivateValue(CommonHooks.class, null, "FORGE_ATTRIBUTES");

        Map<EntityType<? extends LivingEntity>, Builder> finalMap = new HashMap<>();
        ModLoader.postEvent(new EntityAttributeModificationEvent(finalMap));

        finalMap.forEach((k, v) -> {
            AttributeSupplier supplier = DefaultAttributes.getSupplier(k);
            AttributeSupplier.Builder newBuilder = supplier != null ? new AttributeSupplier.Builder(supplier) : new AttributeSupplier.Builder();
            newBuilder.combine(v);
            forgeAttributes.put(k, newBuilder.build());
        });
    }
}
