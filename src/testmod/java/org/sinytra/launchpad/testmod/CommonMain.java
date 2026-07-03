/*
 * Copyright (c) 2026 Sinytra
 * SPDX-License-Identifier: GPL-3.0-only WITH Classpath-exception-2.0
 */

package org.sinytra.launchpad.testmod;

import com.mojang.logging.LogUtils;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.Properties;
import org.sinytra.launchpad.testmod.entity.ModEntityTypes;
import org.slf4j.Logger;

public class CommonMain implements ModInitializer {
    public static final String MODID = "launchpad_testmod";

    public static final Logger LOGGER = LogUtils.getLogger();
    private static boolean initialized;

    public static final ResourceKey<Item> WALRUS_KEY = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MODID, "walrus"));
    public static final Item WALRUS = new Item(new Properties().durability(100).setId(WALRUS_KEY));

    public static final ResourceKey<Registry<Fruit>> FRUITS_KEY = ResourceKey.createRegistryKey(Identifier.fromNamespaceAndPath(MODID, "fruits"));
    public static final Registry<Fruit> FRUITS = FabricRegistryBuilder.create(FRUITS_KEY).buildAndRegister();

    public static final ResourceKey<Fruit> APPLE_KEY = ResourceKey.create(FRUITS_KEY, Identifier.fromNamespaceAndPath(MODID, "apple"));
    public static final Fruit APPLE = new Fruit();

    public static boolean isInitialized() {
        return initialized;
    }

    @Override
    public void onInitialize() {
        LOGGER.info("Called CommonMain#onInitialize!");
        initialized = true;

        // Try registering an item
        Registry.register(BuiltInRegistries.ITEM, WALRUS_KEY, WALRUS);
        Registry.register(FRUITS, APPLE_KEY, APPLE);

        // Register custom entity to test entity attributes initialization
        ModEntityTypes.registerModEntityTypes();
        ModEntityTypes.registerAttributes();

        // Test modify entity attributes event
        FabricDefaultAttributeRegistry.MODIFY.register(context -> {
            context.modify(ModEntityTypes.MINI_GOLEM, (type, builder) -> {
                builder.add(Attributes.TEMPT_RANGE, 2.0);
            });
        });
    }

    public record Fruit() {
    }
}
