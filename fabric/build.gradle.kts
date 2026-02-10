import java.io.FileNotFoundException
import java.nio.file.Files

plugins {
    id("multiloader-loader")
    alias(libs.plugins.loom)
    id("com.modrinth.minotaur") version "2.+"
}

val modName: String by project
val modId: String by project
val version: String by project

base {
    archivesName = "$modId-fabric"
}

modrinth {
    try {
        token = Files.readString(project.rootDir.parentFile.resolve("modrinth_token.txt").toPath())
    } catch (e: FileNotFoundException) {
        e.printStackTrace()
    }

    projectId = "vibrancy"
    versionName = "$modName $version for Fabric 1.21"
    versionNumber = "mc1.21-$version-fabric"
    versionType = "beta"
    uploadFile.set(tasks.remapJar)
    additionalFiles.add(tasks.remapSourcesJar)
    gameVersions.addAll("1.21", "1.21.1")
    loaders.add("fabric")

    dependencies {
        required.project("fabric-api")
        required.project("fabric-language-kotlin")
        required.project("big-shot-lib")
        required.project("fzzy-config")
        required.project("modmenu")
    }
}

repositories {
    mavenCentral()
    maven {
        name = "Modrinth"
        url = uri("https://api.modrinth.com/maven")
    }
}

dependencies {
    minecraft(libs.minecraft)
    mappings(loom.layered {
        officialMojangMappings()
        parchment("org.parchmentmc.data:parchment-${libs.versions.parchmentMC.get()}:${libs.versions.parchment.get()}@zip")
    })
    modImplementation(libs.fabricLoader)
    modImplementation(libs.fabricApi)

    modImplementation(libs.flk)
    modImplementation(libs.sodium)
    modCompileOnly(libs.bigShot)
    modImplementation(libs.fzzyConfig.fabric)
}

loom {
    val aw = project(":common").file("src/main/resources/${modId}.accesswidener")
    if (aw.exists()) {
        accessWidenerPath.set(aw)
    }
    mixin {
        defaultRefmapName.set("${modId}.refmap.json")
    }
    runs {
        named("client") {
            client()
            setConfigName("Fabric Client")
            ideConfigGenerated(true)
            runDir("runs/client")
        }
        named("server") {
            server()
            setConfigName("Fabric Server")
            ideConfigGenerated(true)
            runDir("runs/server")
        }
    }
}