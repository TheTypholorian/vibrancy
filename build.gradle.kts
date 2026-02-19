plugins {
    // see https://fabricmc.net/develop/ for new versions
    alias(libs.plugins.loom) apply false
    // see https://projects.neoforged.net/neoforged/moddevgradle for new versions
    alias(libs.plugins.moddev) apply false
}

subprojects {
    repositories {
        maven {
            name = "FzzyMaven"
            url = uri("https://maven.fzzyhmstrs.me/")
        }

        maven {
            name = "Modrinth"
            url = uri("https://api.modrinth.com/maven")
        }

        ivy {
            url = uri("https://github.com/TheTypholorian/")
            patternLayout {
                artifact("[organisation]/releases/download/[revision]/[artifact]-[revision](-[classifier]).[ext]")
            }
            metadataSources {
                artifact()
            }
        }
    }
}