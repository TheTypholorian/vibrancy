package net.typho.vibrancy.block

import com.google.gson.JsonElement
import com.google.gson.JsonParser.parseReader
import com.google.gson.JsonSyntaxException
import com.mojang.serialization.JsonOps.INSTANCE
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.FileToIdConverter
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.tags.TagKey.create
import net.minecraft.world.level.block.Block
import net.typho.big_shot_lib.resource.SynchronousReloadListener
import net.typho.vibrancy.Vibrancy
import kotlin.jvm.optionals.getOrNull

object BlockLightInfoLoader : SynchronousReloadListener {
    @JvmField
    val singleIdConverter: FileToIdConverter = FileToIdConverter.json("rtx/block_lights/by_block")
    @JvmField
    val tagIdConverter: FileToIdConverter = FileToIdConverter.json("rtx/block_lights/by_block_tag")

    @JvmStatic
    fun load(block: Block, key: ResourceLocation?, json: JsonElement) {
        BlockLightRegistry.blockMap[block] = BlockLightRegistry.infoCodec(block.stateDefinition)
            .codec()
            .parse(INSTANCE, json)
            .getOrThrow { message -> JsonSyntaxException("Error parsing block light info for $key: $message") }
    }

    override fun reload(manager: ResourceManager) {
        BlockLightRegistry.blockMap.clear()

        for (entry in singleIdConverter.listMatchingResources(manager)) {
            entry.value.openAsReader().use { jsonReader ->
                val blockKey = singleIdConverter.fileToId(entry.key)

                if (BuiltInRegistries.BLOCK.containsKey(blockKey)) {
                    val block = BuiltInRegistries.BLOCK.get(blockKey)
                    load(block, blockKey, parseReader(jsonReader))
                }
            }
        }

        for (entry in tagIdConverter.listMatchingResources(manager)) {
            entry.value.openAsReader().use { jsonReader ->
                val blockKey = tagIdConverter.fileToId(entry.key)

                BuiltInRegistries.BLOCK.getTag(create(Registries.BLOCK, blockKey))
                    .ifPresent { tag ->
                        val json = parseReader(jsonReader)

                        tag.forEach { block ->
                            load(block.value(), block.unwrapKey().map { key -> key.location() }.getOrNull(), json)
                        }
                    }
            }
        }

        Vibrancy.LOGGER.info("Loaded ${BlockLightRegistry.blockMap.size} block lights")
    }
}