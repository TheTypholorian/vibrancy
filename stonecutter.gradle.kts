plugins {
    kotlin("jvm") version libs.versions.kotlin apply false
    id("dev.kikugie.stonecutter")
    alias(libs.plugins.loom) apply false
    alias(libs.plugins.moddev) apply false
    id("dev.kikugie.postprocess.jsonlang") version "2.1-beta.4" apply false
    //id("me.modmuss50.mod-publish-plugin") version "0.8.+" apply false
}

stonecutter active "mc1_21_11_fabric"
stonecutter parameters {
    constants.match(node.metadata.project.substringAfterLast('_'), "fabric", "neoforge")
    filters.include("**/*.fsh", "**/*.vsh")
    filters.exclude("**/*.accesswidener")

    replacements.string(current.parsed >= "1.21.5") {
        replace("com.mojang.blaze3d.platform.GlStateManager", "com.mojang.blaze3d.opengl.GlStateManager")
    }
}

//stonecutter tasks {
//    order("publishModrinth")
//    order("publishCurseforge")
//}

//for (version in stonecutter.versions.map { it.version }.distinct()) tasks.register("publish$version") {
//    group = "publishing"
//    dependsOn(stonecutter.tasks.named("publishMods") { metadata.version == version })
//}