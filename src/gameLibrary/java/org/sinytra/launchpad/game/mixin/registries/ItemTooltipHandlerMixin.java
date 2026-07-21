/*
 * Copyright (c) 2026 Sinytra
 * SPDX-License-Identifier: GPL-3.0-only WITH Classpath-exception-2.0
 */

package org.sinytra.launchpad.game.mixin.registries;

import net.minecraft.core.component.DataComponentType;
import net.neoforged.neoforge.common.tooltip.ItemTooltipHandler;
import net.neoforged.neoforge.common.tooltip.TooltipAppender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ItemTooltipHandler.class)
public class ItemTooltipHandlerMixin {
    private static List<DataComponentType<?>> VANILLA_APPENDER_ORDER;
    private static List<TooltipAppender> HEAD_APPENDERS;
    private static List<TooltipAppender> MIDDLE_APPENDERS;
    private static List<TooltipAppender> TAIL_APPENDERS;

    @Inject(method = "init()V", at = @At("HEAD"))
    private static void clearOldValues(CallbackInfo ci) {
        VANILLA_APPENDER_ORDER.clear();
        HEAD_APPENDERS.clear();
        MIDDLE_APPENDERS.clear();
        TAIL_APPENDERS.clear();
    }
}
