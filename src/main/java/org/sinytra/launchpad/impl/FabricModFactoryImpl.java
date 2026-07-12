/*
 * Copyright (c) 2026 Sinytra
 * SPDX-License-Identifier: GPL-3.0-only WITH Classpath-exception-2.0
 */

package org.sinytra.launchpad.impl;

import com.electronwill.nightconfig.core.UnmodifiableCommentedConfig;
import com.electronwill.nightconfig.toml.TomlFormat;
import com.mojang.logging.LogUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.jarcontents.JarContents;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.moddiscovery.ModJarMetadata;
import net.neoforged.fml.loading.moddiscovery.readers.JarModsDotTomlModFileReader;
import net.neoforged.neoforgespi.locating.IModFile;
import net.neoforged.neoforgespi.locating.ModFileDiscoveryAttributes;
import org.jetbrains.annotations.Nullable;
import org.sinytra.launchpad.api.Constants;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.List;
import java.util.jar.Attributes;

public final class FabricModFactoryImpl {
    private static final Logger LOGGER = LogUtils.getLogger();

    @Nullable
    public static IModFile createModFile(JarContents contents, ModFileDiscoveryAttributes discoveryAttributes, @Nullable IModFile.Type type) {
        FabricModMetadata metadata = readModMetadata(contents);
        if (metadata == null) {
            return null;
        }

        Attributes manifest = contents.getManifest().getMainAttributes();
        if (isLibraryType(type) || metadata.isGenerated(manifest)) {
            return IModFile.create(contents, JarModsDotTomlModFileReader::manifestParser, type, discoveryAttributes);
        }

        ModJarMetadata mjm = new ModJarMetadata();
        Dist dist = FMLLoader.getCurrent().getDist();
        IModFile modFile = IModFile.create(contents, mjm, metadata.createNeoMetadataFactory(dist), IModFile.Type.MOD, discoveryAttributes);
        mjm.setModFile(modFile);

        return modFile;
    }

    private static boolean isLibraryType(@Nullable IModFile.Type type) {
        return type == IModFile.Type.LIBRARY || type == IModFile.Type.GAMELIBRARY;
    }

    private static FabricModMetadata readModMetadata(JarContents contents) {
        if (!contents.containsFile(LaunchpadImpl.FMJ) || isNeoForgeMod(contents)) {
            return null;
        }
        return FabricModMetadata.parse(contents);
    }

    private static boolean isNeoForgeMod(JarContents jar) {
        if (jar.containsFile(JarModsDotTomlModFileReader.MODS_TOML)) {
            var modsToml = jar.get(JarModsDotTomlModFileReader.MODS_TOML);
            if (modsToml != null) {
                UnmodifiableCommentedConfig config;
                try (var reader = modsToml.bufferedReader()) {
                    config = TomlFormat.instance().createParser().parse(reader).unmodifiable();
                } catch (IOException e) {
                    LOGGER.error("Failed to read {} from {}", modsToml, jar.getPrimaryPath(), e);
                    return true;
                }
                return !config.contains(List.of("properties", Constants.PLACEHOLDER));
            }
        }
        return false;
    }

    private FabricModFactoryImpl() {
    }
}
