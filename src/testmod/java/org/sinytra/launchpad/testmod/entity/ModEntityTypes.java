/*
 * Copyright (c) 2026 Sinytra
 * SPDX-License-Identifier: GPL-3.0-only WITH Classpath-exception-2.0
 */

package org.sinytra.launchpad.testmod.entity;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import org.sinytra.launchpad.testmod.CommonMain;

public class ModEntityTypes {
	public static final EntityType<MiniGolemEntity> MINI_GOLEM = register(
			"mini_golem",
			EntityType.Builder.<MiniGolemEntity>of(MiniGolemEntity::new, MobCategory.MISC)
                .sized(0.75f, 1.75f)
	);

	private static <T extends Entity> EntityType<T> register(String name, EntityType.Builder<T> builder) {
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(CommonMain.MODID, name));
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
	}

	public static void registerModEntityTypes() {
        CommonMain.LOGGER.info("Registering EntityTypes for " + CommonMain.MODID);
	}

	public static void registerAttributes() {
		FabricDefaultAttributeRegistry.register(MINI_GOLEM, MiniGolemEntity.createAttributes());
	}
}
