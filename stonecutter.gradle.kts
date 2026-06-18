plugins {
    kotlin("jvm") version "2.4.0" apply false
    id("dev.kikugie.stonecutter")
    id("net.fabricmc.fabric-loom") version "1.16-SNAPSHOT" apply false
    id("net.fabricmc.fabric-loom-remap") version "1.16-SNAPSHOT" apply false
    id("net.neoforged.moddev") version "2.0.141" apply false
    id("dev.kikugie.postprocess.jsonlang") version "2.1-beta.4" apply false
}

stonecutter active "mc1_21_1_fabric"
stonecutter handlers {
    inherit("vsh", "glsl")
}
stonecutter parameters {
    constants.match(node.metadata.project.substringAfterLast('_'), "fabric", "neoforge")
    constants.put("sable", findProperty("deps.sable_companion") != null)
    filters.include("**/*.fsh", "**/*.vsh")
}

stonecutter tasks {
    order("publishModrinth")
    //order("publishCurseforge")
}

for (version in stonecutter.versions.map { it.version }.distinct()) tasks.register("publish$version") {
    group = "publishing"
    dependsOn(stonecutter.tasks.named("publishMods") { metadata.version == version })
}