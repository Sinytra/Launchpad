/*
 * Copyright (c) 2026 Sinytra
 * SPDX-License-Identifier: GPL-3.0-only WITH Classpath-exception-2.0
 */

package org.sinytra.launchpad.impl;

import net.neoforged.fml.ModLoadingException;
import net.neoforged.fml.ModLoadingIssue;
import net.neoforged.fml.jarcontents.JarContents;
import net.neoforged.fml.loading.moddiscovery.ModFile;
import net.neoforged.fml.loading.moddiscovery.locators.JarInJarDependencyLocator;
import net.neoforged.jarjar.selection.JarSelector;
import net.neoforged.neoforgespi.locating.*;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.file.Path;
import java.util.*;

@SuppressWarnings({"UnstableApiUsage", "unchecked", "NonExtendableApiUsage"})
public class JarInJarHelpers {
    private static final IDependencyLocator INNER = new JarInJarDependencyLocator();

    private static final MethodHandle LOAD_RES_FROM_FILE;
    private static final MethodHandle LOAD_MOD_FILE_FROM;
    private static final MethodHandle IDENTIFY_MOD;
    private static final MethodHandle EXCEPTION;

    static {
        try {
            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(JarInJarDependencyLocator.class, MethodHandles.lookup());

            LOAD_RES_FROM_FILE = lookup.findVirtual(JarInJarDependencyLocator.class, "loadResourceFromModFile", MethodType.methodType(Optional.class, IModFile.class, String.class));
            LOAD_MOD_FILE_FROM = lookup.findVirtual(JarInJarDependencyLocator.class, "loadModFileFrom", MethodType.methodType(Optional.class, IModFile.class, String.class, IDiscoveryPipeline.class, Map.class));
            IDENTIFY_MOD = lookup.findVirtual(JarInJarDependencyLocator.class, "identifyMod", MethodType.methodType(String.class, IModFile.class));
            EXCEPTION = lookup.findVirtual(JarInJarDependencyLocator.class, "exception", MethodType.methodType(ModLoadingException.class, Collection.class));
        } catch (Exception e) {
            throw new RuntimeException("Error reflecting into JarInJarDependencyLocator", e);
        }
    }

    @Nullable
    public static IModFile loadModFileFrom(IModFile file, String relativePath, IDiscoveryPipeline pipeline, IDependencyLocator locator) {
        IDiscoveryPipeline wrapped = wrapPipeline(pipeline, locator);
        return uncheck(() -> (Optional<IModFile>) LOAD_MOD_FILE_FROM.invoke(INNER, file, relativePath, wrapped, new HashMap<>()))
            .orElse(null);
    }

    public static List<IModFile> scanMods(List<IModFile> loadedMods, IDiscoveryPipeline pipeline, IDependencyLocator locator) {
        IDiscoveryPipeline wrapped = wrapPipeline(pipeline, locator);
        Map<?, IModFile> createdModFiles = new HashMap<>();
        List<IModFile> dependenciesToLoad = uncheck(() ->
            JarSelector.detectAndSelect(
                loadedMods,
                (m, p) -> (Optional<InputStream>) uncheck(() -> LOAD_RES_FROM_FILE.invoke(INNER, m, p)),
                (file, path) -> (Optional<IModFile>) uncheck(() -> LOAD_MOD_FILE_FROM.invoke(INNER, file, path, wrapped, createdModFiles)),
                m -> (String) uncheck(() -> IDENTIFY_MOD.invoke(INNER, m)),
                d -> (ModLoadingException) uncheck(() -> EXCEPTION.invoke(INNER, d))));

        for (var modFile : dependenciesToLoad) {
            if (!pipeline.addModFile(modFile)) {
                ((ModFile) modFile).close();
            }
        }

        return dependenciesToLoad;
    }

    private static <T> T uncheck(CallableThrows<T> callable) {
        try {
            return callable.call();
        } catch (Throwable t) {
            throw new RuntimeException("Error making unsafe call", t);
        }
    }

    @FunctionalInterface
    interface CallableThrows<V> {
        V call() throws Throwable;
    }

    private static IDiscoveryPipeline wrapPipeline(IDiscoveryPipeline inner, IDependencyLocator locator) {
        return new IDiscoveryPipeline() {
            @Override
            public Optional<IModFile> addPath(List<Path> paths, ModFileDiscoveryAttributes attributes, IncompatibleFileReporting reporting) {
                return inner.addPath(paths, attributes, reporting);
            }

            @Override
            public Optional<IModFile> addJarContent(JarContents contents, ModFileDiscoveryAttributes attributes, IncompatibleFileReporting reporting) {
                return inner.addJarContent(contents, attributes, reporting);
            }

            @Override
            public boolean addModFile(IModFile modFile) {
                return inner.addModFile(modFile);
            }

            @Override
            public @Nullable IModFile readModFile(JarContents contents, ModFileDiscoveryAttributes attributes) {
                return inner.readModFile(contents, attributes.withDependencyLocator(locator));
            }

            @Override
            public void addIssue(ModLoadingIssue issue) {
                inner.addIssue(issue);
            }
        };
    }
}
