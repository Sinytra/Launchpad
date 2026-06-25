import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.slf4j.event.Level

plugins {
    `java-library`
    `maven-publish`
    alias(libs.plugins.moddev)
    alias(libs.plugins.shadow) apply false
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

val shade = configurations.create("shade")
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
    shade(libs.forgified.fabric.loader)
    shade(libs.clazz.tweaker) { isTransitive = false }

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

val depsJar = tasks.register("depsJar", ShadowJar::class) {
    configurations = listOf(shade)
    archiveClassifier.set("deps")
}

val gameLibraryJar = tasks.register("gameLibraryJar", Jar::class) {
    from(gameLibrary.output)

    manifest.attributes("Implementation-Version" to project.version)
    manifest.from("src/gameLibrary/resources/META-INF/MANIFEST.MF")

    archiveClassifier.set("game")
}
localJarJar(
    "gameLibraryLocalJarJar",
    "org.sinytra.launchpad:game-library",
    project.version.toString(),
    gameLibraryJar
)

val dummyLoaderJar = tasks.register("dummyLoaderJar", Jar::class) {
    manifest.attributes(
        "FMLModType" to "LIBRARY",
        "Automatic-Module-Name" to "net.fabricmc.loader",
        "Implementation-Version" to "999.999.999",
    )
    archiveClassifier.set("dummy")
}
localJarJar(
    "dummyFabricLoaderLocalJarJar",
    "org.sinytra:forgified-fabric-loader",
    "999.999.999",
    dummyLoaderJar
)

val fullJar = tasks.register("fullJar", ShadowJar::class) {
    from(
        depsJar.flatMap { it.archiveFile.map(::zipTree) },
        tasks.jar.flatMap { it.archiveFile.map(::zipTree) }
    )

    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    mergeServiceFiles()
    filesNotMatching("META-INF/services/**") { duplicatesStrategy = DuplicatesStrategy.FAIL }

    relocate("net.fabricmc.classtweaker", "org.sinytra.launchpad.reloc.net.fabricmc.classtweaker")
    manifest.attributes(tasks.jar.get().manifest.attributes)
    archiveClassifier.set("full")

    doLast {
        val githubOutput = System.getenv("GITHUB_OUTPUT")
        if (githubOutput != null) {
            File(githubOutput).appendText("PRIMARY_ARTIFACT=${archiveFile.get().asFile.absolutePath}")
        }
    }
}

tasks {
    withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
    }

    named<Wrapper>("wrapper") {
        distributionType = Wrapper.DistributionType.BIN
    }

    assemble {
        dependsOn(fullJar)
    }
}

configurations.runtimeElements {
    setExtendsFrom(emptySet())
    outgoing {
        artifacts.clear()
        artifact(fullJar)
    }
}

publishing {
    publications {
        register<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }

    repositories {
        val env = System.getenv()
        if (env["MAVEN_URL"] != null) {
            repositories.maven {
                url = uri(env["MAVEN_URL"] as String)
                if (env["MAVEN_USERNAME"] != null) {
                    credentials {
                        username = env["MAVEN_USERNAME"]
                        password = env["MAVEN_PASSWORD"]
                    }
                }
            }
        }
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
