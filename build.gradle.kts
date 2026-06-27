import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import me.modmuss50.mpp.ReleaseType
import me.modmuss50.mpp.platforms.modrinth.ModrinthEnvironment
import org.slf4j.event.Level

plugins {
    `java-library`
    `maven-publish`
    alias(libs.plugins.moddev)
    alias(libs.plugins.shadow) apply false
    alias(libs.plugins.gradleutils)
    alias(libs.plugins.licenser)
    alias(libs.plugins.publishing)
    idea
}

val mod_group_id: String by project
val mod_id: String by project
val mod_name: String by project
val mod_license: String by project
val mod_license_spdx: String by project
val neo_version: String by project
val minecraft_version: String by project
val launchpad_version: String by project

val compatible_versions: String by project
val curseforge_id: String by project
val modrinth_id: String by project
val github_repo: String by project
val publish_branch: String by project

val PUBLISH_RELEASE_TYPE = providers.environmentVariable("PUBLISH_RELEASE_TYPE")

group = mod_group_id
version = "$launchpad_version+$minecraft_version"
// Append git commit hash for dev versions
if (!PUBLISH_RELEASE_TYPE.isPresent) {
    version = "$version+dev-${gradleutils.gitInfo["hash"]}"
}
println("Version: $version")

base {
    archivesName = mod_id
}

java.toolchain.languageVersion = JavaLanguageVersion.of(25)

val shade = configurations.create("shade")
val gameLibrary = sourceSets.create("gameLibrary")
val testmod = sourceSets.create("testmod")

neoForge {
    version = neo_version

    addModdingDependenciesTo(gameLibrary)
    addModdingDependenciesTo(testmod)
    mods {
        create(mod_id) {
            sourceSet(sourceSets.main.get())
        }
        create("${mod_id}_game") {
            sourceSet(gameLibrary)
        }
        create("${mod_id}_test") {
            sourceSet(sourceSets.test.get())
        }
        create("${mod_id}_testmod") {
            sourceSet(testmod)
        }
    }

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
            loadedMods = loadedMods.get().filter { !it.name.contains("test") }
        }
    }


    unitTest {
        enable()
        testedMod = mods.named(mod_id)
    }
}

license {
    header = resources.text.fromFile("HEADER")
    skipExistingHeaders = true
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

    "testmodImplementation"(libs.forgified.fabric.loader)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.platform.launcher)
    testImplementation(libs.testframework)
    testImplementation(testmod.output)
}

listOf(sourceSets.main.get(), testmod).forEach { sourceSet ->
    val taskName = sourceSet.getTaskName("generate", "ModMetadata")
    val generateModMetadata = tasks.register<ProcessResources>(taskName) {
        val replaceProperties = mapOf(
            "mod_id" to mod_id,
            "mod_name" to mod_name,
            "mod_license" to mod_license,
            "mod_license_spdx" to mod_license_spdx,
            "mod_version" to project.version,
        )
        inputs.properties(replaceProperties)
        expand(replaceProperties)
        from("src/${sourceSet.name}/templates")
        into("build/generated/sources/${sourceSet.name}/modMetadata")
    }
    sourceSet.resources.srcDir(generateModMetadata)
    neoForge.ideSyncTask(generateModMetadata)
}

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

    test {
        useJUnitPlatform()
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

publishMods {
    file = fullJar.flatMap { it.archiveFile }
    changelog = providers.environmentVariable("CHANGELOG").orElse("# ${project.version}")
    type = PUBLISH_RELEASE_TYPE.orElse("alpha").map(ReleaseType::of)
    modLoaders = listOf("neoforge")
    dryRun = !providers.environmentVariable("CI").isPresent
    displayName = "[$minecraft_version] Launchpad ${project.version}"

    val compatibleVersions = compatible_versions.split(",")

    github {
        accessToken = providers.environmentVariable("GITHUB_TOKEN")
        repository = github_repo
        commitish = publish_branch
    }
    curseforge {
        accessToken = providers.environmentVariable("CURSEFORGE_TOKEN")
        projectId = curseforge_id
        minecraftVersions = compatibleVersions
        client = true
        server = true
    }
    modrinth {
        accessToken = providers.environmentVariable("MODRINTH_TOKEN")
        projectId = modrinth_id
        minecraftVersions = compatibleVersions
        environment = ModrinthEnvironment.CLIENT_OR_SERVER
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
