@file:Suppress("UnstableApiUsage")

import io.github.klahap.dotenv.DotEnvBuilder
import net.fabricmc.loom.task.RemapJarTask
import net.fabricmc.loom.task.RemapSourcesJarTask

plugins {
    kotlin("jvm")
    alias(libs.plugins.loom)
    id("dev.kikugie.postprocess.jsonlang")
    id("me.modmuss50.mod-publish-plugin") version "2.0.0-beta.1"
    id("com.google.devtools.ksp") version "2.2.0-2.0.2"
    id("dev.kikugie.fletching-table.fabric") version "0.1.0-alpha.22"
    id("io.github.klahap.dotenv") version "1.1.3"
}

fletchingTable {
    mixins.create("main") {
        mixin("default", "${project.property("mod.id")}.mixins.json")
    }
}

sourceSets {
    main {
        java {
            if (sc.current.parsed < "1.21.5") {
                exclude("net/typho/vibrancy/mixin/GlTextureAccessor.java")
            }
        }
    }
}

val env = DotEnvBuilder.dotEnv {
    addFile("$rootDir/.env")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xjvm-default=all")
    }
}

tasks.withType<RemapJarTask> {
    destinationDirectory.set(rootProject.file("build/libs/${project.version}"))
}

tasks.withType<RemapSourcesJarTask> {
    destinationDirectory.set(rootProject.file("build/libs/${project.version}"))
}

tasks.named<ProcessResources>("processResources") {
    val props = HashMap<String, String>().apply {
        this["minecraft"] = (project.property("deps.minecraft_range") ?: project.property("deps.minecraft")) as String
        this["java"] = when {
            sc.current.parsed >= "1.20.5" -> "21"
            sc.current.parsed >= "1.18" -> "17"
            sc.current.parsed >= "1.17" -> "16"
            else -> "8"
        }
        this["mod_id"] = project.property("mod.id") as String
        this["mod_name"] = project.property("mod.name") as String
        this["mod_version"] = project.property("mod.version") as String
        this["mod_author"] = project.property("mod.author") as String
        this["mod_description"] = project.property("mod.description") as String
        this["mod_credits"] = project.property("mod.credits") as String
        this["mod_license"] = project.property("mod.license") as String
        this["big_shot_version"] = libs.bigShot.get().version as String
    }

    inputs.properties(props)

    filesMatching(listOf("fabric.mod.json", "META-INF/neoforge.mods.toml", "META-INF/mods.toml", "**/*.mixins.json")) {
        expand(props)
    }
}

version = "${property("mod.version")}+${property("deps.minecraft")}-fabric"
base.archivesName = property("mod.id") as String

jsonlang {
    languageDirectories = listOf("assets/${property("mod.id")}/lang")
    prettyPrint = true
}

repositories {
    mavenLocal()
    maven("https://thedarkcolour.github.io/KotlinForForge/") { name = "KotlinForForge" }
    maven {
        name = "Modrinth"
        url = uri("https://api.modrinth.com/maven")
    }
    maven("https://maven.isxander.dev/releases") {
        name = "Xander Maven"
    }
    maven("https://maven.ryanhcode.dev/releases") {
        name = "RyanHCode Maven"
    }
    maven {
        name = "Fabric"
        url = uri("https://maven.fabricmc.net")
    }

    ivy {
        url = uri("https://github.com/TheTypholorian/big_shot_lib/releases/download")
        patternLayout {
            artifact("[revision]/[artifact]-[revision](-[classifier]).[ext]")
        }
        metadataSources {
            artifact()
        }
    }
}

tasks.withType<Javadoc>().configureEach {
    enabled = false
}

dependencies {
    minecraft("com.mojang:minecraft:${property("deps.minecraft")}")
    mappings(loom.layered {
        officialMojangMappings()
        if (hasProperty("deps.parchment"))
            parchment("org.parchmentmc.data:parchment-${property("deps.parchment")}@zip")
    })
    modImplementation("net.fabricmc:fabric-loader:0.19.2")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${property("deps.fabric-api")}")
    modImplementation(libs.flk)

    modImplementation(libs.sodium)
    modImplementation(libs.bigShot)
    modImplementation(libs.yacl)
    modCompileOnly(libs.modmenu)

    if (sc.current.version == "1.21") {
        include(libs.sableCompanionFabric)
        modApi(libs.sableCompanionFabric)
    }
}

fabricApi {
    configureDataGeneration() {
        outputDirectory = file("$rootDir/src/main/generated")
        //client = true
    }
}

tasks {
    processResources {
        exclude("**/neoforge.mods.toml", "**/mods.toml")
    }

    register<Copy>("buildAndCollect") {
        group = "build"
        from(remapJar.map { it.archiveFile })
        into(rootProject.layout.buildDirectory.file("libs/${project.property("mod.version")}"))
        dependsOn("build")
    }
}

java {
    val javaCompat = when {
        sc.current.parsed >= "1.20.5" -> JavaVersion.VERSION_21
        sc.current.parsed >= "1.18" -> JavaVersion.VERSION_17
        sc.current.parsed >= "1.17" -> JavaVersion.VERSION_16
        else -> JavaVersion.VERSION_1_8
    }
    sourceCompatibility = javaCompat
    targetCompatibility = javaCompat
}

kotlin {
    jvmToolchain(
        when {
            sc.current.parsed >= "1.20.5" -> 21
            sc.current.parsed >= "1.18" -> 17
            sc.current.parsed >= "1.17" -> 16
            else -> 8
        }
    )
}

val additionalVersionsStr = findProperty("publish.additionalVersions") as String?
val additionalVersions: List<String> = additionalVersionsStr
    ?.split(",")
    ?.map { it.trim() }
    ?.filter { it.isNotEmpty() }
    ?: emptyList()

publishMods {
    file = tasks.remapJar.map { it.archiveFile.get() }
    additionalFiles.from(tasks.remapSourcesJar.map { it.archiveFile.get() })

    type = STABLE
    displayName = "${property("mod.name")} ${property("mod.version")} for ${stonecutter.current.version} Fabric"
    version = "${property("mod.version")}+${stonecutter.current.version}-fabric"
    changelog = ""
    //changelog = provider { rootProject.file("CHANGELOG.md").readText() }
    modLoaders.add("fabric")

    modrinth {
        projectId = property("publish.modrinth") as String
        accessToken = env["MODRINTH_TOKEN"]
        minecraftVersions.add(stonecutter.current.version)
        minecraftVersions.addAll(additionalVersions)
        requires("fabric-api", "fabric-language-kotlin", "big-shot-lib", "yacl")
    }

    /*
    curseforge {
        projectId = property("publish.curseforge") as String
        accessToken = env["CURSEFORGE_TOKEN"]
        minecraftVersions.add(stonecutter.current.version)
        minecraftVersions.addAll(additionalVersions)
        requires("fabric-api", "fabric-language-kotlin", "big-shot-lib", "yacl")
    }
     */
}