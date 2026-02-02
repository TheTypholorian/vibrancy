import org.gradle.internal.extensions.stdlib.capitalized
import java.io.FileNotFoundException
import java.nio.file.Files

plugins {
    id("multiloader-loader")
    alias(libs.plugins.moddev)
    id("com.modrinth.minotaur") version "2.+"
}

val modName: String by project
val modId: String by project
val version: String by project

base {
    archivesName = "$modId-neoforge"
}

modrinth {
    try {
        token = Files.readString(project.rootDir.parentFile.resolve("modrinth_token.txt").toPath())
    } catch (e: FileNotFoundException) {
        e.printStackTrace()
    }

    projectId = "vibrancy"
    versionName = "$modName $version for NeoForge 1.21"
    versionNumber = "mc1.21-$version-neoforge"
    versionType = "beta"
    uploadFile.set(tasks.jar)
    additionalFiles.add(tasks.sourcesJar)
    gameVersions.addAll("1.21", "1.21.1")
    loaders.add("neoforge")

    dependencies {
        required.project("kotlin-for-forge")
        required.project("big-shot-lib")
        required.project("fzzy-config")
    }
}

neoForge {
    version = libs.versions.neoforge
    // Automatically enable neoforge AccessTransformers if the file exists
    val at = project(":common").file("src/main/resources/META-INF/accesstransformer.cfg")
    if (at.exists()) {
        accessTransformers.from(at.absolutePath)
    }
    parchment {
        minecraftVersion = libs.versions.parchmentMC
        mappingsVersion = libs.versions.parchment
    }
    runs {
        configureEach {
            systemProperty("neoforge.enabledGameTestNamespaces", modId)
            ideName = "NeoForge ${name.capitalized()} (${project.path})" // Unify the run config names with fabric
        }
        register("client") {
            client()
        }
        register("data") {
            data()
        }
        register("server") {
            server()
        }
    }
    mods {
        register(modId) {
            sourceSet(sourceSets.main.get())
        }
    }
}

sourceSets.main.get().resources { srcDir("src/generated/resources") }

dependencies {
    implementation(libs.kff)
    implementation(libs.sodium)
    implementation(libs.bigShot.neoforge)
    implementation(libs.fzzyConfig.neoforge)
}