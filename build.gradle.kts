import org.slf4j.event.Level

plugins {
    `java-library`
    `maven-publish`
    id("net.neoforged.moddev") version "2.0.141"
    idea
}

val mod_version: String by project
val mod_group_id: String by project
val mod_id: String by project
val mod_name: String by project
val mod_license: String by project
val neo_version: String by project
val minecraft_version: String by project
val minecraft_version_range: String by project

version = mod_version
group = mod_group_id

base {
    archivesName = mod_id
}

java.toolchain.languageVersion = JavaLanguageVersion.of(25)

val gameLibrary = sourceSets.create("gameLibrary")

neoForge {
    version = neo_version
    
    addModdingDependenciesTo(gameLibrary)

    runs {
        create("client") {
            client()
        }

        create("server") {
            server()
            programArgument("--nogui")
        }

        configureEach {
            systemProperty("mixin.debug.export", "true")

            logLevel = Level.DEBUG
        }
    }

    mods {
        create(mod_id) {
            sourceSet(sourceSets.main.get())
        }
        create("$mod_id-game") {
            sourceSet(gameLibrary)
        }
    }
}

val localRuntime = configurations.create("localRuntime")
configurations {
    runtimeClasspath.get().extendsFrom(localRuntime)
}

repositories {
    maven {
        name = "Sinytra"
        url = uri("https://maven.sinytra.org")
    }
    maven {
        name = "FabricMC"
        url = uri("https://maven.fabricmc.net")
    }
    mavenLocal()
}

dependencies {
    implementation(libs.forgified.fabric.loader)
    implementation(libs.clazz.tweaker)

    "gameLibraryImplementation"(libs.forgified.fabric.loader)
    "gameLibraryImplementation"(sourceSets.main.get().output)
}

val generateModMetadata = tasks.register<ProcessResources>("generateModMetadata") {
    val replaceProperties = mapOf(
        "mod_id" to mod_id,
        "mod_name" to mod_name,
        "mod_license" to mod_license,
        "mod_version" to mod_version,
    )
    inputs.properties(replaceProperties)
    expand(replaceProperties)
    from("src/main/templates")
    into("build/generated/sources/modMetadata")
}
sourceSets.main {
    resources.srcDir(generateModMetadata)
}
neoForge.ideSyncTask(generateModMetadata)

tasks {
    withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
    }

    named<Wrapper>("wrapper") {
        distributionType = Wrapper.DistributionType.BIN
    }
}

val gameLibraryJar = tasks.register("gameLibraryJar", Jar::class) {
    from(gameLibrary.output)
    manifest.attributes("Implementation-Version" to project.version)
    archiveClassifier.set("game")
}
localJarJar(
    "modJarConfig",
    "org.sinytra.launchpad:game-library",
    project.version.toString(),
    gameLibraryJar
)

publishing {
    publications {
        register<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
    repositories {
        mavenLocal()
    }
}

idea {
    module {
        isDownloadSources = true
        isDownloadJavadoc = true
    }
}

fun localJarJar(configName: String, mavenCoords: String, version: String, artifact: Any) {
    configurations.create(configName) {
        attributes {
            attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(Usage.JAVA_RUNTIME))
        }
        outgoing {
            artifact(artifact)
            capability("$mavenCoords:$version")
        }
    }
    dependencies {
        jarJar(project(":")) { capabilities { requireCapability(mavenCoords) } }
    }
}
