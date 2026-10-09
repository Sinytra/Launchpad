/*
 * Copyright (c) 2026 Sinytra
 * SPDX-License-Identifier: GPL-3.0-only WITH Classpath-exception-2.0
 */

package org.sinytra.launchpad.test;

import net.minecraft.server.MinecraftServer;
import net.neoforged.testframework.junit.EphemeralTestServerProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sinytra.launchpad.testmod.Server;

import java.lang.reflect.AccessFlag;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(EphemeralTestServerProvider.class)
public class ClassTweakerTest {
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
    void testInterfaceInjection(MinecraftServer server) {
        Server instance = (Server) server;
        assertEquals(server, instance.getSelf());
    }
}
