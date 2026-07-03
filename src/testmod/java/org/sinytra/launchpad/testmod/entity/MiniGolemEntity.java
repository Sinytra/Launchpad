/*
 * Copyright (c) 2026 Sinytra
 * SPDX-License-Identifier: GPL-3.0-only WITH Classpath-exception-2.0
 */

package org.sinytra.launchpad.testmod.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

public class MiniGolemEntity extends PathfinderMob {
    public MiniGolemEntity(Level world) {
        this(ModEntityTypes.MINI_GOLEM, world);
    }

    public MiniGolemEntity(EntityType<? extends MiniGolemEntity> entityType, Level world) {
        super(entityType, world);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 5)
            .add(Attributes.TEMPT_RANGE, 10)
            .add(Attributes.MOVEMENT_SPEED, 0.3);
    }
}