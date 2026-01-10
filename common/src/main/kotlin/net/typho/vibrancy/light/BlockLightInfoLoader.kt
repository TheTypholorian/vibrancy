package net.typho.vibrancy.light

import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException
import com.mojang.serialization.JsonOps
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.FileToIdConverter
import net.minecraft.server.packs.resources.ResourceManager
import net.typho.big_shot_lib.resource.SynchronousReloadListener
import net.typho.vibrancy.Vibrancy

object BlockLightInfoLoader : SynchronousReloadListener {
    @JvmField
    val idConverter: FileToIdConverter = FileToIdConverter.json("block_lights")

    override fun reload(manager: ResourceManager) {
        BlockLightInfo.MAP.clear()

        for (entry in idConverter.listMatchingResources(manager)) {
            entry.value.openAsReader().use { jsonReader ->
                val blockKey = idConverter.fileToId(entry.key)
                val block = BuiltInRegistries.BLOCK.get(blockKey)
                BlockLightInfo.MAP.put(
                    block,
                    BlockLightInfo.codec(block.stateDefinition)
                        .codec()
                        .parse(
                            JsonOps.INSTANCE,
                            JsonParser.parseReader(jsonReader)
                        )
                        .getOrThrow { message -> JsonSyntaxException("Error parsing block light info for $blockKey: $message") }
                )
            }
        }

        Vibrancy.LOGGER.info("Loaded ${BlockLightInfo.MAP.size} block lights")
    }
}