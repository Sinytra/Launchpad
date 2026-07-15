/*
 * Copyright (c) 2026 Sinytra
 * SPDX-License-Identifier: GPL-3.0-only WITH Classpath-exception-2.0
 */

package org.sinytra.launchpad.test;

import io.netty.buffer.Unpooled;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.inventory.RecipeBookType;
import net.neoforged.neoforge.network.connection.ConnectionType;
import net.neoforged.testframework.junit.EphemeralTestServerProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sinytra.launchpad.testmod.CommonMain;
import org.sinytra.launchpad.testmod.CommonMain.Fruit;
import org.sinytra.launchpad.testmod.PrelaunchMain;
import org.sinytra.launchpad.testmod.entity.ModEntityTypes;

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

    @Test
    void testCustomRegistryExists(MinecraftServer server) {
        assertNotNull(
            BuiltInRegistries.REGISTRY.getValue(CommonMain.FRUITS_KEY.identifier()),
            "Expected custom registry to be registered"
        );
    }

    @Test
    void testModifiedAttributes(MinecraftServer server) {
        AttributeSupplier supplier = DefaultAttributes.getSupplier(ModEntityTypes.MINI_GOLEM);

        assertNotNull(supplier, "Expected supplier to exist");
        assertTrue(supplier.hasAttribute(Attributes.TEMPT_RANGE), "Expected entity to contain attribute");
        assertEquals(2.0, supplier.getBaseValue(Attributes.TEMPT_RANGE), "Expected base value");
    }

    @Test
    void testByteBufCodecNonSyncedRegistry(MinecraftServer server) {
        StreamCodec<RegistryFriendlyByteBuf, Fruit> codec = ByteBufCodecs.registry(CommonMain.FRUITS_KEY);

        RegistryFriendlyByteBuf output = new RegistryFriendlyByteBuf(Unpooled.buffer(), server.registryAccess(), ConnectionType.OTHER);
        assertDoesNotThrow(() -> codec.encode(output, CommonMain.APPLE));
    }

    @Test
    void testFluidTypePolyfill() {
        assertNotNull(CommonMain.HONEY.getFluidType());
    }

    @Test
    void testEnumExtensionMetadata() {
        assertNotNull(RecipeBookType.valueOf("LAUNCHPAD_TESTMOD_TEST"));
    }
}
