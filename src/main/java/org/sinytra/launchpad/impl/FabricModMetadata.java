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
import java.util.*;
import java.util.Map.Entry;
import java.util.jar.Attributes;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.sinytra.launchpad.api.Constants.ENABLE_LAUNCHPAD;
import static org.sinytra.launchpad.api.Constants.OVERRIDES;

public class FabricModMetadata {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String LOOM_GENERATED_PROPERTY = "fabric-loom:generated";
    private static final String LOOM_REMAP_ATTRIBUTE = "Fabric-Loom-Remap";
    private static final Pattern SUBSTITUTION = Pattern.compile("^\\$\\{(.+)}$");

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
            JsonObject rootCopy = root.deepCopy();

            JsonObject custom = root.getAsJsonObject("custom");

            // Check if Launchpad opt-in is enabled
            boolean load = Optional.ofNullable(custom)
                .flatMap(c -> Optional.ofNullable(c.getAsJsonPrimitive(ENABLE_LAUNCHPAD)))
                .map(JsonPrimitive::getAsBoolean)
                .orElse(false);
            if (!load) {
                return null;
            }
            
            if (custom != null) {
                custom.addProperty(LaunchpadImpl.LAUNCHPAD_ACTIVE, true);
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
                    String key = entry.getKey();
                    JsonElement value = entry.getValue();
                    JsonElement modified = processOverride(key, value, rootCopy);
                    root.add(key, modified == null ? value : modified);
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

    private static JsonElement processOverride(String key, JsonElement value, JsonObject root) {
        if (value.isJsonObject()) {
            return processObjectOverride(value.getAsJsonObject(), root);
        } else if (value.isJsonArray()) {
            return processArrayOverride(value.getAsJsonArray(), root);
        } else {
            return processOverrideValue(key, value, root);
        }
    }

    private static JsonElement processObjectOverride(JsonObject parent, JsonObject root) {
        for (Entry<String, JsonElement> entry : Set.copyOf(parent.entrySet())) {
            JsonElement replaced = processOverride(entry.getKey(), entry.getValue(), root);
            if (replaced != null) {
                parent.add(entry.getKey(), replaced);
            }
        }
        return parent;
    }

    private static JsonElement processArrayOverride(JsonArray parent, JsonObject root) {
        List<JsonElement> copyOf = List.copyOf(parent.asList());
        for (int i = 0; i < copyOf.size(); i++) {
            JsonElement element = copyOf.get(i);
            JsonElement replaced = processOverride(String.valueOf(i), element, root);
            if (replaced != null) {
                parent.set(i, replaced);
            }
        }
        return parent;
    }

    @Nullable
    private static JsonElement processOverrideValue(String key, JsonElement value, JsonObject root) {
        if (!value.isJsonPrimitive()) {
            return null;
        }

        JsonPrimitive primitive = value.getAsJsonPrimitive();
        if (!primitive.isString()) {
            return null;
        }

        String rawValue = primitive.getAsString();
        Matcher matcher = SUBSTITUTION.matcher(rawValue);
        if (!matcher.matches()) {
            return null;
        }

        String[] path = matcher.group(1).split("\\.");
        if (path.length == 0) {
            return value;
        }

        try {
            return resolvePath(root, path);
        } catch (Exception e) {
            throw new RuntimeException("Error substituting variable override for %s".formatted(key), e);
        }
    }

    private static JsonElement resolvePath(JsonObject root, String[] path) {
        if (path.length == 0) {
            throw new IllegalArgumentException("Path must not be empty");
        }

        JsonElement result = root.get(path[0]);
        if (result == null) {
            throw new NullPointerException("Invalid json path");
        }

        for (int i = 1; i < path.length; i++) {
            String key = path[i];

            if (result.isJsonObject()) {
                result = result.getAsJsonObject().get(key);
            } else if (result.isJsonArray()) {
                int index = Integer.parseInt(key);
                result = result.getAsJsonArray().get(index);
            } else {
                throw new IllegalArgumentException("Can only index object and array json elements");
            }

            if (result == null) {
                throw new NullPointerException("Invalid json path");
            }
        }

        return result;
    }
}
