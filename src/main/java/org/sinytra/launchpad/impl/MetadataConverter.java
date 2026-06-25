package org.sinytra.launchpad.impl;

import com.electronwill.nightconfig.core.Config;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.metadata.ContactInformation;
import net.fabricmc.loader.api.metadata.Person;
import net.fabricmc.loader.impl.metadata.LoaderModMetadata;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.moddiscovery.ModFile;
import net.neoforged.fml.loading.moddiscovery.ModFileInfo;
import net.neoforged.fml.loading.moddiscovery.NightConfigWrapper;
import net.neoforged.neoforgespi.language.IConfigurable;
import net.neoforged.neoforgespi.language.IModFileInfo;
import net.neoforged.neoforgespi.locating.IModFile;
import org.slf4j.Logger;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
        config.add("modproperties", modid);

        Config modListConfig = config.createSubConfig();
        modListConfig.add("modId", modid);

        String version = normalizeVersion(metadata.getVersion().getFriendlyString());
        // Validate version string. If it's invalid, we'll let FML assign a default version instead
        if (VALID_VERSION.matcher(version).matches()) {
            modListConfig.add("version", version);
        } else {
            LOGGER.warn("Ignoring invalid version for mod {} in file {}", modid, modFile.getFilePath());
        }

        modListConfig.add("displayName", metadata.getName());
        modListConfig.add("description", metadata.getDescription());
        metadata.getIconPath(-1).ifPresent(icon -> modListConfig.add("logoFile", icon));

        ContactInformation contact = metadata.getContact();
        contact.get("homepage")
            .or(() -> contact.get("source"))
            .or(() -> Optional.of(contact.asMap())
                .filter(m -> !m.isEmpty())
                .map(m -> m.entrySet().iterator().next().getValue()))
            // Ensure string is valid url
            .filter(MetadataConverter::isValidURL)
            .ifPresent(url -> {
                modListConfig.add("modUrl", url);
                modListConfig.add("displayURL", url);
            });

        contact.get("issues")
            .filter(MetadataConverter::isValidURL)
            .ifPresent(url -> modListConfig.add("issueTrackerURL", url));

        modListConfig.add("authors", metadata.getAuthors().stream()
            .map(Person::getName)
            .collect(Collectors.joining(", ")));

        modListConfig.add("credits", metadata.getContributors().stream()
            .map(Person::getName)
            .collect(Collectors.joining(", ")));

        config.add("mods", List.of(modListConfig));

        List<Config> mixins = metadata.getMixinConfigs(distToEnv(dist)).stream()
            .map(str -> {
                Config mixinConfig = modListConfig.createSubConfig();
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

    private MetadataConverter() {}
}
