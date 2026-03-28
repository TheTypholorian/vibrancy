package net.typho.vibrancy.block

import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.google.gson.JsonParser
import com.mojang.serialization.JsonOps
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.typho.big_shot_lib.api.util.WrapperUtil
import net.typho.big_shot_lib.api.util.resources.*
import net.typho.vibrancy.Vibrancy

object BlockLightInfoLoader : NeoResourceManagerReloadListener {
    @JvmField
    val singleIdConverter = NeoFileToIdConverter.json("rtx/block_lights/by_block")
    @JvmField
    val tagIdConverter = NeoFileToIdConverter.json("rtx/block_lights/by_block_tag")
    private val warned = HashSet<ResourceIdentifier>()

    @JvmStatic
    fun load(block: Block, key: ResourceIdentifier, json: JsonElement, file: ResourceIdentifier) {
        val typeKey = ResourceIdentifier.CODEC.decode(JsonOps.INSTANCE, json.asJsonObject.get("type"))
            .getOrThrow { JsonParseException("Error while parsing block light info $file: $it") }
            .first
        val codec = (BlockLightRegistry.registry!!.get(typeKey) ?: throw JsonParseException("No block light type $typeKey"))
                .infoCodec(block.stateDefinition)
        BlockLightRegistry.blockMap[block] = codec.codec()
            .parse(JsonOps.INSTANCE, json)
            .getOrThrow { message -> JsonParseException("Error parsing block light info for $key: $message") }
    }

    override fun onResourceManagerReload(manager: NeoResourceManager) {
        BlockLightRegistry.blockMap.clear()
        val blocks = WrapperUtil.INSTANCE.wrap(BuiltInRegistries.BLOCK)

        for (entry in singleIdConverter.listMatchingResources(manager)) {
            entry.value.openAsReader().use { jsonReader ->
                val blockKey = singleIdConverter.fileToId(entry.key)
                val block = blocks.get(blockKey)?.let { if (it == Blocks.AIR) null else it }

                if (block == null) {
                    if (warned.add(blockKey)) {
                        Vibrancy.LOGGER.warn("Couldn't find block $blockKey to give a block light to")
                    }
                } else {
                    load(block, blockKey, JsonParser.parseReader(jsonReader), entry.key)
                }
            }
        }

        for (entry in tagIdConverter.listMatchingResources(manager)) {
            entry.value.openAsReader().use { jsonReader ->
                blocks.getTag(NeoTagKey(blocks.key().location, tagIdConverter.fileToId(entry.key)))?.let { tag ->
                    val json = JsonParser.parseReader(jsonReader)

                    tag.forEach { block ->
                        load(block, blocks.getKey(block).location, json, entry.key)
                    }
                }
            }
        }

        Vibrancy.LOGGER.info("Loaded ${BlockLightRegistry.blockMap.size} block lights")
        Vibrancy.lightManager.reload()
    }
}