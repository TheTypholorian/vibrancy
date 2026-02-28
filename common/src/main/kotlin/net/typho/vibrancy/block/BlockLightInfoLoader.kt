package net.typho.vibrancy.block

import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException
import com.mojang.serialization.JsonOps
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.level.block.Block
import net.typho.big_shot_lib.api.util.WrapperUtil
import net.typho.big_shot_lib.api.util.resources.*
import net.typho.vibrancy.Vibrancy

object BlockLightInfoLoader : NeoResourceManagerReloadListener {
    @JvmField
    val singleIdConverter = NeoFileToIdConverter.json("rtx/block_lights/by_block")
    @JvmField
    val tagIdConverter = NeoFileToIdConverter.json("rtx/block_lights/by_block_tag")

    @JvmStatic
    fun load(block: Block, key: ResourceIdentifier, json: JsonElement) {
        BlockLightRegistry.blockMap[block] = BlockLightRegistry.infoCodec(block.stateDefinition)
            .codec()
            .parse(JsonOps.INSTANCE, json)
            .getOrThrow { message -> JsonSyntaxException("Error parsing block light info for $key: $message") }
    }

    override fun onResourceManagerReload(manager: NeoResourceManager) {
        BlockLightRegistry.blockMap.clear()
        val blocks = WrapperUtil.INSTANCE.wrap(BuiltInRegistries.BLOCK)

        for (entry in singleIdConverter.listMatchingResources(manager)) {
            entry.value.openAsReader().use { jsonReader ->
                val blockKey = singleIdConverter.fileToId(entry.key)

                if (blocks.contains(blockKey)) {
                    val block = blocks.get(blockKey) ?: throw NullPointerException("Cannot find block $blockKey")
                    load(block, blockKey, JsonParser.parseReader(jsonReader))
                }
            }
        }

        for (entry in tagIdConverter.listMatchingResources(manager)) {
            entry.value.openAsReader().use { jsonReader ->
                val blockKey = tagIdConverter.fileToId(entry.key)

                blocks.getTag(NeoTagKey(ResourceIdentifier("blocks"), blockKey))?.let { tag ->
                    val json = JsonParser.parseReader(jsonReader)

                    tag.forEach { block ->
                        load(block, blocks.getKey(block).location, json)
                    }
                }
            }
        }

        Vibrancy.LOGGER.info("Loaded ${BlockLightRegistry.blockMap.size} block lights")
    }
}