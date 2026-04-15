plugins {
    // see https://fabricmc.net/develop/ for new versions
    alias(libs.plugins.loom) apply false
    // see https://projects.neoforged.net/neoforged/moddevgradle for new versions
    alias(libs.plugins.moddev) apply false
}

subprojects {
    repositories {
        maven {
            name = "Modrinth"
            url = uri("https://api.modrinth.com/maven")
        }
        maven("https://maven.isxander.dev/releases") {
            name = "Xander Maven"
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
}