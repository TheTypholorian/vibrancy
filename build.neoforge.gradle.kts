plugins {
    kotlin("jvm")
    id("net.neoforged.moddev")
    id("dev.kikugie.postprocess.jsonlang")
    //id("me.modmuss50.mod-publish-plugin")
    id("com.google.devtools.ksp") version "2.2.0-2.0.2"
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xjvm-default=all")
    }
}

tasks.withType<Jar> {
    destinationDirectory.set(rootProject.file("build/libs/${project.version}"))
}

val processResources = tasks.named<ProcessResources>("processResources") {
    val props = HashMap<String, String>().apply {
        this["minecraft"] = (project.property("deps.minecraft_range") ?: "[${project.property("deps.minecraft")}]") as String
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

version = "${property("mod.version")}+${property("deps.minecraft")}-neoforge"
base.archivesName = property("mod.id") as String

jsonlang {
    languageDirectories = listOf("assets/${property("mod.id")}/lang")
    prettyPrint = true
}

neoForge {
    version = property("deps.neoforge") as String

    if (hasProperty("deps.parchment")) parchment {
        val (mc, ver) = (property("deps.parchment") as String).split(':')
        mappingsVersion = ver
        minecraftVersion = mc
    }

    runs {
        register("client") {
            gameDirectory = file("run/")
            client()
        }
        register("server") {
            gameDirectory = file("run/")
            server()
        }
    }

    mods {
        register(property("mod.id") as String) {
            sourceSet(sourceSets["main"])
        }
    }
    sourceSets["main"].resources.srcDir("src/main/generated")
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

dependencies {
    implementation(libs.kff)

    implementation(libs.sodium)
    implementation(libs.bigShot)
    implementation(libs.yacl)
    implementation(libs.modmenu)
    jarJar(libs.sableCompanion)
    api(libs.sableCompanion)
}

tasks {
    jar {
        exclude("**/*.accesswidener")
    }

    processResources {
        exclude("**/fabric.mod.json", "**/mods.toml")
    }

    named("createMinecraftArtifacts") {
        dependsOn("stonecutterGenerate")
    }

    register<Copy>("buildAndCollect") {
        group = "build"
        from(jar.map { it.archiveFile })
        into(rootProject.layout.buildDirectory.file("libs/${project.property("mod.version")}"))
        dependsOn("build")
    }
}

java {
    val javaCompat = if (stonecutter.eval(stonecutter.current.version, ">=1.20.5")) {
        JavaVersion.VERSION_21
    } else {
        JavaVersion.VERSION_17
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

/*
publishMods {
    file = tasks.jar.map { it.archiveFile.get() }
    additionalFiles.from(tasks.named<org.gradle.jvm.tasks.Jar>("sourcesJar").map { it.archiveFile.get() })

    type = BETA
    displayName = "${property("mod.name")} ${property("mod.version")} for ${stonecutter.current.version} Neoforge"
    version = "${property("mod.version")}+${stonecutter.current.version}-neoforge"
    changelog = provider { rootProject.file("CHANGELOG.md").readText() }
    modLoaders.add("neoforge")

    modrinth {
        projectId = property("publish.modrinth") as String
        accessToken = env.MODRINTH_API_KEY.orNull()
        minecraftVersions.add(stonecutter.current.version)
        minecraftVersions.addAll(additionalVersions)
        requires("kotlin-for-forge")
    }

    curseforge {
        projectId = property("publish.curseforge") as String
        accessToken = env.CURSEFORGE_API_KEY.orNull()
        minecraftVersions.add(stonecutter.current.version)
        minecraftVersions.addAll(additionalVersions)
        requires("kotlin-for-forge")
    }
}
 */