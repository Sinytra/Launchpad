/*
 * Copyright (c) 2026 Sinytra
 * SPDX-License-Identifier: GPL-3.0-only WITH Classpath-exception-2.0
 */

package org.sinytra.launchpad.testmod;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.fabricmc.fabric.api.item.v1.DefaultItemComponentEvents;
import net.fabricmc.fabric.api.item.v1.ItemComponentTooltipProviderRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import org.sinytra.launchpad.testmod.entity.ModEntityTypes;
import org.sinytra.launchpad.testmod.fluid.HoneyFluid;
import org.sinytra.launchpad.testmod.tooltip.TestComponent;
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

    public static final ResourceKey<Fluid> HONEY_KEY = ResourceKey.create(Registries.FLUID, Identifier.fromNamespaceAndPath(MODID, "honey"));
    public static final Fluid HONEY = new HoneyFluid();

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
        Registry.register(BuiltInRegistries.FLUID, HONEY_KEY, HONEY);

        // Register custom entity to test entity attributes initialization
        ModEntityTypes.registerModEntityTypes();
        ModEntityTypes.registerAttributes();

        // Register tooltip providers
        initTooltipProviders();

        // Test modify entity attributes event
        FabricDefaultAttributeRegistry.MODIFY.register(context -> {
            context.modify(ModEntityTypes.MINI_GOLEM, (type, builder) -> {
                builder.add(Attributes.TEMPT_RANGE, 2.0);
            });
        });
    }

    private void initTooltipProviders() {
        DataComponentType<TestComponent> happyComponent = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            MODID + ":happy_component",
            DataComponentType.<TestComponent>builder()
                .persistent(MapCodec.unitCodec(TestComponent.HAPPY))
                .build()
        );

        ItemComponentTooltipProviderRegistry.addBefore(DataComponents.UNBREAKABLE, happyComponent);

        DefaultItemComponentEvents.MODIFY.register(context -> {
            context.modify(Items.DIAMOND_SWORD, builder -> builder.set(happyComponent, TestComponent.HAPPY));
        });
    }

    public record Fruit() {
    }
}
