/*
 * Copyright (c) 2026 Sinytra
 * SPDX-License-Identifier: GPL-3.0-only WITH Classpath-exception-2.0
 */

package org.sinytra.launchpad.testmod;

import com.mojang.logging.LogUtils;
import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.Properties;
import org.slf4j.Logger;

public class CommonMain implements ModInitializer {
    public static final String MODID = "launchpad_testmod";
    
    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean initialized;
    
    public static final ResourceKey<Item> WALRUS_KEY = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MODID, "walrus"));
    public static final Item WALRUS = new Item(new Properties().durability(100).setId(WALRUS_KEY));

    public static boolean isInitialized() {
        return initialized;
    }

    @Override
    public void onInitialize() {
        LOGGER.info("Called CommonMain#onInitialize!");
        initialized = true;
        
        // Try registering an item
        Registry.register(BuiltInRegistries.ITEM, WALRUS_KEY, WALRUS);
    }
}
