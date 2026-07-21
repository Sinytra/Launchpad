/*
 * Copyright (c) 2026 Sinytra
 * SPDX-License-Identifier: GPL-3.0-only WITH Classpath-exception-2.0
 */

package org.sinytra.launchpad.testmod.tooltip;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.component.TooltipProvider;

public interface TestComponent extends TooltipProvider {
    TestComponent HAPPY = (context, consumer, flag, components) ->
        consumer.accept(Component.literal("This Item is Happy"));
}
