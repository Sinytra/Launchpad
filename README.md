# Launchpad

[![Build](https://github.com/Sinytra/Launchpad/actions/workflows/build.yml/badge.svg)](https://github.com/Sinytra/Launchpad/actions/workflows/build.yml)
[![Latest Release](https://img.shields.io/github/v/release/Sinytra/Launchpad?style=flat&label=Release&include_prereleases&sort=semver)](https://github.com/Sinytra/Launchpad/releases/latest)
[![CurseForge](https://cf.way2muchnoise.eu/short_1584761.svg)](https://www.curseforge.com/minecraft/mc-mods/launchpad)
[![Modrinth](https://img.shields.io/modrinth/dt/voWgQoWV?logo=modrinth&label=Modrinth&color=00AF5C)](https://modrinth.com/project/launchpad)
[![Discord](https://discordapp.com/api/guilds/1141048834177388746/widget.png?style=shield)](https://discord.sinytra.org)

A small tool for developing NeoForge mods using Fabric conventions.

- [About](#about)
  - [When to use Launchpad](#when-to-use-launchpad) 
  - [When not to use Launchpad](#when-not-to-use-launchpad)
- [Installation](#installation)
- [Usage](#usage)
  - [Property overrides](#property-overrides)
  - [NeoForge Placeholder](#neoforge-placeholder)
- [Environment](#environment)
  - [Metadata](#metadata) 
  - [Entrypoints](#entrypoints)
  - [Registration](#registration)
  - [Fabric loader](#fabric-loader)
  - [Caveats](#caveats)
- [API](#api)
- [License](#license)

## About

Launchpad's main features include the following:

- Reading mod metadata from `fabric.mod.json` files
- Running mod entrypoints (both standard and custom)
- Access transformation using Class Tweakers (or Access Wideners)
- Loading nested jar dependencies

Furthermore, we provide certain extensions on top of the Fabric standard described in [Usage](#usage).  

### When to use Launchpad

Generally speaking, Launchpad works best for small to medium-sized Fabric mods that are already potentially compatible
with NeoForge, just need that small final push to get them loaded.

Use Launchpad, when:

- You want to port your mod to NeoForge but don't have the time for a full, native port
- Your mod requires little to no changes to be compatible with NeoForge (mainly speaking of Mixins)
- You're fine with having extra dependencies on NeoForge, namely the
[Forgified Fabric API](https://github.com/Sinytra/ForgifiedFabricAPI) and Launchpad

### When not to use Launchpad

Consider making a native port or using the [MultiLoader template](https://github.com/jaredlll08/MultiLoader-Template),
when:

- You want to use NeoForge APIs extensively
- You want full control over your metadata or entrypoints
- You don't want extra dependencies

## Installation

Install Launchpad in your development environment by including it in your dependencies. You can find available versions
on our [maven](https://maven.su5ed.dev/#/releases/org/sinytra/launchpad/launchpad) or on GitHub
[releases](https://github.com/Sinytra/Launchpad/releases).

Use the following code to add Launchpad to a ModDevGradle workspace:

```groovy
repositories {
    maven {
        name = "Sinytra"
        url = "https://maven.sinytra.org/"
    }
}

dependencies {
    implementation "org.sinytra.launchpad:launchpad:<version>"
}
```

## Usage

Running mods on Launchpad requires explicit opt-in from the mod author. Launchpad automatically scans Fabric mods
located by FML and uses a custom metatada property to decide whether they should be loaded.

To enable loading your mod on Launchpad, add the following to your `fabric.mod.json` file.

```json
{
  "custom": {
    "launchpad:compatible": true
  }
}
```

From here on, Launchpad will handle everything else for you.

### Property overrides

Property overrides can be used to provide different values for a property depending on the mod loader used. Launchpad
will overwrite the actual value with its override counterpart when it reads the metadata file.

Example use cases include changing dependencies or the mod ID.

You can define overrides using the `launchpad:overrides` custom property. Its body follows the same format as the
outer Fabric Mod Json.

```json5
{
  // NeoForge doesn't allow '-' in mod IDs
  "id": "example-mod",
  "custom": {
    "launchpad:overrides": {
      // Swap the mod ID for a valid one on NeoForge
      "id": "example_mod"
    }
  }
}
```

### NeoForge Placeholder

If a user installs a Launchpad-compatible Fabric mod, but forgets to install Launchpad itself, FML has no way to tell
them about the missing dependency, and will instead show an error screen saying the mod is invalid.

To overcome this issue, Launchpad offers a placeholder/stub feature that allows developers to safely depend on Launchpad
and notify users about missing dependencies when Launchpad is not installed. To enable this, include a standard
`neoforge.mods.toml` metadata file in your mod jar. In addition to the mod declaration, add the following code:

```toml
[properties]
"launchpad:placeholder"=true
```

At discovery time, Launchpad will recognize this as a placeholder, ignore the NeoForge metadata file and load your mod
using `fabric.mod.json` instead.

In addition to the property above, don't forget to actually declare a dependency on Launchpad. For convenience, you can
use this snippet: 
```toml
# Replace with your actual mod id
[[dependencies.your_mod_id]]
modId="launchpad"
type="required"
versionRange="[0,)"
ordering="NONE"
side="BOTH"
```

## Environment

Launchpad is designed to be a developer porting tool, not a compatibility layer. Unlike Connector, it makes no
modifications to the mod's code and only converts metadata files to NeoForge's format at runtime. No changes are made
to the mod's jar, either.

This means resolving potential compatibility issues, such as in Mixins, is the left up to mod authors.
In return, we can provide a very minimal and stable tool for running their mods on NeoForge without making any invasive
and unpredictable changes to them that the author can't control.

Launchpad will, however, do its best to provide runtime conditions replicating those of Fabric as closely as possible.
This mainly concerns game object registration logic, which differs significantly on NeoForge. More on that below.

### Metadata

Fabric metadata is translated to NeoForge as accurately as possible, but due to the differences in the two formats,
the result may be missing information from properties that don't have a NeoForge counterpart.

Dependencies will be translated as well, with dependency resolution being handled by FML natively.

As a special integration with the Forgified Fabric API, mod IDs of Fabric API modules in dependencies will be
automatically replaced with FFAPI counterparts (`fabric-api` -> `fabric_api`). This is done to avoid having to declare
redundant overrides in mod metadata.

### Entrypoints

The `main`, `client` and `server` entrypoint will run at the same time as on Fabric.

The `preLaunch` entrypoint is a special case because it runs before the game's entrypoint is called. On Launchpad, we
invoke it from inside a Mixin config plugin's static initialization block. We guarantee that the most common use cases
for this entrypoint, such as loading native libraries or additional Mixin config files, work correctly.

### Registration

Usually, registering game objects on NeoForge must be done inside the `RegisterEvent`, during which the registries are
unfrozen. However, Fabric mods run out their registration logic in their initializers. At the time of their invocation,
the registries are frozen and NeoForge's builtin registry callbacks haven't been added yet.

Launchpad resolves these issues and ensures the registries are ready to receive registration calls when a mod's
entrypoints run.

### Fabric loader

Launchpad makes Fabric Loader API available using our NeoForge port of the Fabric Loader,
[Forgified Fabric Loader](https://github.com/Sinytra/ForgifiedFabricLoader). This is also used to manage the metadata
and entrypoints of mods loaded via Launchpad, but also to provide Fabric-facing information of native FML mods.
As a result, Fabric mods can access the metadata of both "Fabric" and FML mods through just the Fabric Loader.

### Caveats

Despite our best efforts, certain environmental settings may still differ. If you run into any issues, please file a
bug report!

## API

To integrate with Launchpad programatically, you can use the API provided in the `org.sinytra.launchpad.api` package.

## License

Launchpad is licensed under the GNU General Public License v3 with the Classpath Exception v2.0. See LICENSE for the
full license text.

