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

neoForge {
    version = neo_version

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
    }
}

val localRuntime by configurations.creating
configurations {
    runtimeClasspath.get().extendsFrom(localRuntime)
}

repositories {
    maven {
        name = "Sinytra"
        url = uri("https://maven.sinytra.org")
    }
}

dependencies {
    implementation(libs.forgified.fabric.loader)
}

val generateModMetadata = tasks.register<ProcessResources>("generateModMetadata") {
    val replaceProperties = mapOf(
        "minecraft_version" to minecraft_version,
        "minecraft_version_range" to minecraft_version_range,
        "neo_version" to neo_version,
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

// Example configuration to allow publishing using the maven-publish plugin
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

tasks {
    withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
    }

    named<Wrapper>("wrapper") {
        distributionType = Wrapper.DistributionType.BIN
    }
}

idea {
    module {
        isDownloadSources = true
        isDownloadJavadoc = true
    }
}
