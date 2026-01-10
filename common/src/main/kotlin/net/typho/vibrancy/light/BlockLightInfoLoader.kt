package net.typho.vibrancy.light

import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException
import com.mojang.serialization.JsonOps
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.FileToIdConverter
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.tags.TagKey
import net.minecraft.world.level.block.Block
import net.typho.big_shot_lib.resource.SynchronousReloadListener
import net.typho.vibrancy.Vibrancy
import java.io.Reader

object BlockLightInfoLoader : SynchronousReloadListener {
    @JvmField
    val idConverter: FileToIdConverter = FileToIdConverter.json("block_lights")

    fun load(block: Block, key: ResourceLocation, reader: Reader) {
        BlockLightInfo.MAP.put(
            block,
            BlockLightInfo.codec(block.stateDefinition)
                .codec()
                .parse(
                    JsonOps.INSTANCE,
                    JsonParser.parseReader(reader)
                )
                .getOrThrow { message -> JsonSyntaxException("Error parsing block light info for $key: $message") }
        )
    }

    override fun reload(manager: ResourceManager) {
        BlockLightInfo.MAP.clear()

        for (entry in idConverter.listMatchingResources(manager)) {
            entry.value.openAsReader().use { jsonReader ->
                var blockKey = idConverter.fileToId(entry.key)

                if (blockKey.path.startsWith("tag/")) {
                    blockKey = blockKey.withPath { path -> path.substring("tag/".length) }

                    BuiltInRegistries.BLOCK.getTag(TagKey.create(Registries.BLOCK, blockKey))
                        .ifPresent { tag ->
                            tag.forEach { block ->
                                load(
                                    block.value(),
                                    block.unwrapKey()
                                        .map { key -> key.location() }
                                        .orElse(blockKey),
                                    jsonReader
                                )
                            }
                        }
                } else if (BuiltInRegistries.BLOCK.containsKey(blockKey)) {
                    load(BuiltInRegistries.BLOCK.get(blockKey), blockKey, jsonReader)
                }
            }
        }

        Vibrancy.LOGGER.info("Loaded ${BlockLightInfo.MAP.size} block lights")
    }
}