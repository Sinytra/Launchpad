/*
 * Copyright (c) 2026 Sinytra
 * SPDX-License-Identifier: GPL-3.0-only WITH Classpath-exception-2.0
 */

package org.sinytra.launchpad.test;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.neoforged.testframework.junit.EphemeralTestServerProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sinytra.launchpad.testmod.CommonMain;
import org.sinytra.launchpad.testmod.PrelaunchMain;

import java.lang.reflect.AccessFlag;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(EphemeralTestServerProvider.class)
public class LaunchpadTest {
    private static final String MODID = "launchpad_testmod";
    private static final String OG_MODID = "launchpad-testmod";
    private static final String PROVIDED_MODID = "examplemod";
    
    @Test
    void testPreLaunchEntrypoint(MinecraftServer server) {
        assertTrue(PrelaunchMain.isInitialized(), "Expected preLaunch entrypoint to have been called");
    }

    @Test
    void testMainEntrypoint(MinecraftServer server) {
        assertTrue(CommonMain.isInitialized(), "Expected main entrypoint to have been called");
    }

    @Test
    void testClassAccessTransformer(MinecraftServer server) throws Exception {
        Class<?> cls = Class.forName("net.minecraft.util.Crypt$ByteArrayToKeyFunction");
        Assertions.assertTrue(cls.accessFlags().contains(AccessFlag.PUBLIC), "Expected class to be public");
    }

    @Test
    void testMethodAccessTransformer(MinecraftServer server) throws Exception {
        Class<?> cls = Class.forName("net.minecraft.util.Util");
        Method method = cls.getDeclaredMethod("makeExecutor", String.class);
        assertEquals(Set.of(AccessFlag.PUBLIC, AccessFlag.STATIC), method.accessFlags());
    }

    @Test
    void testFieldAccessTransformer(MinecraftServer server) throws Exception {
        Class<?> cls = Class.forName("net.minecraft.server.MinecraftServer");
        Field field = cls.getDeclaredField("random");
        assertEquals(field.accessFlags(), Set.of(AccessFlag.PUBLIC));
    }

    @Test
    void testRegisteredItem(MinecraftServer server) {
        Item item = BuiltInRegistries.ITEM.getValue(CommonMain.WALRUS_KEY);
        assertNotNull(item, "Expected item to be registered");
    }

    @Test
    void testModAlias(MinecraftServer server) {
        ModContainer mod = FabricLoader.getInstance().getModContainer(MODID).orElseThrow();
        ModContainer original = FabricLoader.getInstance().getModContainer(OG_MODID).orElseThrow();
        ModContainer provided = FabricLoader.getInstance().getModContainer(PROVIDED_MODID).orElseThrow();

        assertEquals(mod, original, "Expected mod containers to be the same");
        assertEquals(mod, provided, "Expected mod containers to be the same");
    }
}
