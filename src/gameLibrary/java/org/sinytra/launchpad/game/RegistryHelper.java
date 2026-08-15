/*
 * Copyright (c) 2026 Sinytra
 * SPDX-License-Identifier: GPL-3.0-only WITH Classpath-exception-2.0
 */

package org.sinytra.launchpad.game;

import com.mojang.logging.LogUtils;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.ModLoader;
import net.neoforged.fml.util.ObfuscationReflectionHelper;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.CreativeModeTabRegistry;
import net.neoforged.neoforge.common.tooltip.ItemTooltipHandler;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import org.slf4j.Logger;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RegistryHelper {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static void postSetup() {
        // Post DataPackRegistryEvent.NewRegistry
        try {
            postDataPackRegistryEvent();
        } catch (Exception e) {
            LOGGER.error("Error posting DataPackRegistryEvent.NewRegistry event", e);
        }

        // Post RegisterEvent for COMMAND_ARGUMENT_TYPE
        try {
            postRegisterCommandsEvent();
        } catch (Exception e) {
            LOGGER.error("Error posting RegisterEvent event for COMMAND_ARGUMENT_TYPE", e);
        }

        // Post EntityAttributeModificationEvent
        try {
            postModifyEntityAttributes();
        } catch (Exception e) {
            LOGGER.error("Error posting EntityAttributeModificationEvent", e);
        }

        // Sort creative tabs including Fabric tabs
        try {
            resortCreativeTabs();
        } catch (Exception e) {
            LOGGER.error("Error sorting creative tabs", e);
        }

        // Re-init tooltip appenders to account for Fabric mods
        try {
            reinitTooltipAppenders();
        } catch (Exception e ){
            LOGGER.error("Error initializing tooltip appenders", e);
        }
    }

    private static void postDataPackRegistryEvent() throws Exception {
        ModContainer container = ModList.get().getModContainerById("fabric_registry_sync_v0").orElse(null);
        if (container != null) {
            DataPackRegistryEvent.NewRegistry event = new DataPackRegistryEvent.NewRegistry();
            container.getEventBus().post(event);

            Method method = DataPackRegistryEvent.NewRegistry.class.getDeclaredMethod("process");
            method.setAccessible(true);
            method.invoke(event);
        }
    }

    private static void postRegisterCommandsEvent() throws Exception {
        ModContainer container = ModList.get().getModContainerById("fabric_command_api_v2").orElse(null);
        if (container != null) {
            Constructor<?> ctr = ObfuscationReflectionHelper.findConstructor(RegisterEvent.class, ResourceKey.class, Registry.class);
            ctr.setAccessible(true);

            RegisterEvent event = (RegisterEvent) ctr.newInstance(Registries.COMMAND_ARGUMENT_TYPE, BuiltInRegistries.COMMAND_ARGUMENT_TYPE);
            container.getEventBus().post(event);
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

    private static void resortCreativeTabs() {
        List<CreativeModeTab> defaultTabs =
            ObfuscationReflectionHelper.getPrivateValue(CreativeModeTabRegistry.class, null, "DEFAULT_TABS");

        defaultTabs.clear();

        CreativeModeTabRegistry.sortTabs();
    }
    
    private static void reinitTooltipAppenders() {
        ItemTooltipHandler.init();
    }
}
