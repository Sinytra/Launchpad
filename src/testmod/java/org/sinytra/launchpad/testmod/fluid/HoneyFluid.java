/*
 * Copyright (c) 2026 Sinytra
 * SPDX-License-Identifier: GPL-3.0-only WITH Classpath-exception-2.0
 */

package org.sinytra.launchpad.testmod.fluid;

import net.minecraft.world.level.material.EmptyFluid;
import net.minecraft.world.level.material.FluidState;

public class HoneyFluid extends EmptyFluid {
    @Override
    public boolean isSource(FluidState fluidState) {
        return true;
    }
}
