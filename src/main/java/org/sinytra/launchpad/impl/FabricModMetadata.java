/*
 * Copyright (c) 2026 Sinytra
 * SPDX-License-Identifier: GPL-3.0-only WITH Classpath-exception-2.0
 */

package org.sinytra.launchpad.impl;

import com.google.gson.*;
import com.mojang.logging.LogUtils;
import net.fabricmc.loader.api.metadata.CustomValue;
import net.fabricmc.loader.impl.metadata.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.jarcontents.JarContents;
import net.neoforged.fml.jarcontents.JarResource;
import net.neoforged.fml.loading.LogMarkers;
import net.neoforged.fml.util.PathPrettyPrinting;
import net.neoforged.neoforgespi.locating.ModFileInfoParser;
import org.jetbrains.annotations.Nullable;
import org.sinytra.launchpad.service.FabricModJsonFileReader;
import org.slf4j.Logger;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.jar.Attributes;

import static org.sinytra.launchpad.api.Constants.ENABLE_LAUNCHPAD;
import static org.sinytra.launchpad.api.Constants.OVERRIDES;

public class FabricModMetadata {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String LOOM_GENERATED_PROPERTY = "fabric-loom:generated";
    private static final String LOOM_REMAP_ATTRIBUTE = "Fabric-Loom-Remap";

    private final LoaderModMetadata metadata;

    public FabricModMetadata(LoaderModMetadata metadata) {
        this.metadata = metadata;
    }

    @Nullable
    public static FabricModMetadata parse(JarContents contents) {
        Path path = contents.getPrimaryPath();

        JarResource modsJson = contents.get(FabricModJsonFileReader.FMJ);
        if (modsJson == null) {
            LOGGER.warn(LogMarkers.LOADING, "Mod file {} is missing {} file", path, FabricModJsonFileReader.FMJ);
            return null;
        }

        JsonElement element = preProcess(modsJson, path);
        if (element == null) {
            // Missing Launchpad opt-in
            return null;
        }
        String json = new Gson().toJson(element);

        try (InputStream ins = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8))) {
            LoaderModMetadata metadata = ModMetadataParser.parseMetadata(
                ins,
                PathPrettyPrinting.prettyPrint(path),
                Collections.emptyList(),
                new VersionOverrides(),
                new DependencyOverrides(Path.of("nonexistent")),
                false
            );

            return new FabricModMetadata(metadata);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read " + modsJson + " from " + path, e);
        } catch (ParseMetadataException e) {
            throw new RuntimeException("Malformed " + modsJson + " from " + path, e);
        }
    }

    @Nullable
    private static JsonElement preProcess(JarResource resource, Path path) {
        try (Reader reader = resource.bufferedReader()) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();

            JsonObject custom = root.getAsJsonObject("custom");

            // Check if Launchpad opt-in is enabled
            boolean load = Optional.ofNullable(custom)
                .flatMap(c -> Optional.ofNullable(c.getAsJsonPrimitive(ENABLE_LAUNCHPAD)))
                .map(JsonPrimitive::getAsBoolean)
                .orElse(false);
            if (!load) {
                return null;
            }

            JsonObject overrides = custom != null ? custom.getAsJsonObject(OVERRIDES) : null;
            if (overrides != null) {
                // Add original mod ID to provides 
                if (overrides.has("id")) {
                    JsonArray provides = Objects.requireNonNullElseGet(root.getAsJsonArray("provides"), JsonArray::new);

                    JsonPrimitive modId = Objects.requireNonNull(root.getAsJsonPrimitive("id"), "Missing mod ID");
                    provides.add(modId.getAsString());

                    root.add("provides", provides);
                }

                for (Entry<String, JsonElement> entry : overrides.entrySet()) {
                    root.add(entry.getKey(), entry.getValue());
                }
            }

            return root;
        } catch (Exception e) {
            throw new RuntimeException("Failed to read " + resource + " from " + path, e);
        }
    }

    public boolean isGenerated(Attributes manifestAttributes) {
        CustomValue generatedValue = this.metadata.getCustomValue(LOOM_GENERATED_PROPERTY);
        if (generatedValue != null && generatedValue.getType() == CustomValue.CvType.BOOLEAN && generatedValue.getAsBoolean()) {
            String loomRemapAttribute = manifestAttributes.getValue(LOOM_REMAP_ATTRIBUTE);
            return loomRemapAttribute == null || !loomRemapAttribute.equals("true");
        }
        return false;
    }

    public ModFileInfoParser createNeoMetadataFactory(Dist dist) {
        return file -> MetadataConverter.createNeoMetadata(this.metadata, file, dist);
    }
}
