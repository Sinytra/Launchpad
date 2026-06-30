/*
 * Copyright (c) 2026 Sinytra
 * SPDX-License-Identifier: GPL-3.0-only WITH Classpath-exception-2.0
 */

package org.sinytra.launchpad.impl;

import com.electronwill.nightconfig.core.Config;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.metadata.ContactInformation;
import net.fabricmc.loader.api.metadata.ModDependency;
import net.fabricmc.loader.api.metadata.ModDependency.Kind;
import net.fabricmc.loader.api.metadata.Person;
import net.fabricmc.loader.impl.metadata.LoaderModMetadata;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.moddiscovery.ModFile;
import net.neoforged.fml.loading.moddiscovery.ModFileInfo;
import net.neoforged.fml.loading.moddiscovery.NightConfigWrapper;
import net.neoforged.neoforgespi.language.IConfigurable;
import net.neoforged.neoforgespi.language.IModFileInfo;
import net.neoforged.neoforgespi.language.IModInfo.DependencySide;
import net.neoforged.neoforgespi.language.IModInfo.DependencyType;
import net.neoforged.neoforgespi.language.IModInfo.Ordering;
import net.neoforged.neoforgespi.locating.IModFile;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.sinytra.launchpad.impl.LaunchpadImpl.FABRIC_METADATA;
import static org.sinytra.launchpad.impl.LaunchpadImpl.LAUNCHPAD_ACTIVE;

public final class MetadataConverter {
    private static final Logger LOGGER = LogUtils.getLogger();

    // From ModInfo
    private static final Pattern VALID_VERSION = Pattern.compile("^\\d+.*");
    private static final String DEFAULT_LICENSE = "All Rights Reserved";

    public static IModFileInfo createNeoMetadata(LoaderModMetadata metadata, IModFile modFile, Dist dist) {
        String modid = metadata.getId();

        Config config = Config.inMemory();
        config.add("modLoader", "javafml");
        config.add("loaderVersion", "[0, )");
        Collection<String> licenses = metadata.getLicense()
            .stream().map(String::trim).filter(l -> !l.isBlank())
            .toList();
        config.add("license", licenses.isEmpty() ? DEFAULT_LICENSE : String.join(", ", metadata.getLicense()));

        config.add("properties", Map.of(
            FABRIC_METADATA, metadata,
            LAUNCHPAD_ACTIVE, true
        ));

        Config modConfig = config.createSubConfig();
        modConfig.add("modId", modid);

        String version = normalizeVersion(metadata.getVersion().getFriendlyString());
        // Validate version string. If it's invalid, we'll let FML assign a default version instead
        if (VALID_VERSION.matcher(version).matches()) {
            modConfig.add("version", version);
        } else {
            LOGGER.warn("Ignoring invalid version for mod {} in file {}", modid, modFile.getFilePath());
        }

        modConfig.add("displayName", metadata.getName());
        modConfig.add("description", metadata.getDescription());
        metadata.getIconPath(-1).ifPresent(icon -> modConfig.add("logoFile", icon));

        ContactInformation contact = metadata.getContact();
        contact.get("homepage")
            .or(() -> contact.get("source"))
            .or(() -> Optional.of(contact.asMap())
                .filter(m -> !m.isEmpty())
                .map(m -> m.entrySet().iterator().next().getValue()))
            // Ensure string is valid url
            .filter(MetadataConverter::isValidURL)
            .ifPresent(url -> {
                modConfig.add("modUrl", url);
                modConfig.add("displayURL", url);
            });

        contact.get("issues")
            .filter(MetadataConverter::isValidURL)
            .ifPresent(url -> modConfig.add("issueTrackerURL", url));

        modConfig.add("authors", metadata.getAuthors().stream()
            .map(Person::getName)
            .collect(Collectors.joining(", ")));

        modConfig.add("credits", metadata.getContributors().stream()
            .map(Person::getName)
            .collect(Collectors.joining(", ")));

        config.add("mods", List.of(modConfig));

        if (!metadata.getDependencies().isEmpty()) {
            List<Config> depConfigs = new ArrayList<>();
            Config features = Config.inMemory();

            for (ModDependency dependency : metadata.getDependencies()) {
                // Convert the java dependency to a javaVersion feature requirement (see https://docs.neoforged.net/docs/gettingstarted/modfiles/#features)
                if (dependency.getModId().equals("java")) {
                    features.add("javaVersion", VersionConverter.convert(dependency.getVersionRequirements()));
                    continue;
                }

                if (dependency.getModId().equals("fabricloader")) {
                    features.add("fabricLoader", VersionConverter.convert(dependency.getVersionRequirements()));
                    continue;
                }

                Config depConfig = convertDependency(dependency);
                if (depConfig != null) {
                    depConfigs.add(depConfig);
                }
            }

            if (!depConfigs.isEmpty()) {
                config.add(List.of("dependencies", modid), depConfigs);
            }
            if (!features.isEmpty()) {
                config.add(List.of("features", modid), features);
            }
        }

        List<Config> mixins = metadata.getMixinConfigs(distToEnv(dist)).stream()
            .map(str -> {
                Config mixinConfig = modConfig.createSubConfig();
                mixinConfig.add("config", str);
                return mixinConfig;
            })
            .toList();
        if (!mixins.isEmpty()) {
            config.add("mixins", mixins);
        }

        IConfigurable configurable = new NightConfigWrapper(config);
        return new ModFileInfo((ModFile) modFile, configurable, f -> {
        }, List.of());
    }

    @Nullable
    private static Config convertDependency(ModDependency dependency) {
        Config config = Config.inMemory();

        config.add("modId", convertDepModId(dependency.getModId()));
        config.add("type", convertDepType(dependency.getKind()).name().toLowerCase(Locale.ROOT));
        config.add("versionRange", VersionConverter.convert(dependency.getVersionRequirements()));
        config.add("ordering", Ordering.NONE.name());
        config.add("side", DependencySide.BOTH.name());

        return config;
    }

    private static String convertDepModId(String modId) {
        // Hardcoded conversion for Forgified Fabric API modules
        if (modId.equals("fabric-api") || modId.matches("^fabric-.+v\\d$")) {
            return modId.replace('-', '_');
        }
        return modId;
    }

    private static DependencyType convertDepType(Kind kind) {
        return switch (kind) {
            case DEPENDS -> DependencyType.REQUIRED;
            case RECOMMENDS, SUGGESTS -> DependencyType.OPTIONAL;
            case CONFLICTS -> DependencyType.DISCOURAGED;
            case BREAKS -> DependencyType.INCOMPATIBLE;
        };
    }

    private static String normalizeVersion(String version) {
        return version.replace("+", "_");
    }

    private static boolean isValidURL(String str) {
        if (str == null) {
            return false;
        }
        try {
            new URL(str);
            return true;
        } catch (MalformedURLException e) {
            return false;
        }
    }

    private static EnvType distToEnv(Dist dist) {
        return switch (dist) {
            case CLIENT -> EnvType.CLIENT;
            case DEDICATED_SERVER -> EnvType.SERVER;
        };
    }

    private MetadataConverter() {
    }
}
