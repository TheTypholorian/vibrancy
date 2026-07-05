package net.typho.vibrancy.util

import net.caffeinemc.mods.sodium.client.render.texture.SpriteContentsExtension
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite
import net.minecraft.client.renderer.texture.SpriteLoader
import net.minecraft.client.renderer.texture.TextureAtlas
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.client.renderer.texture.atlas.SpriteResourceLoader
import net.minecraft.client.renderer.texture.atlas.SpriteSourceList
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection
import net.minecraft.resources.FileToIdConverter
import net.minecraft.resources.Identifier
import net.minecraft.server.packs.resources.ResourceManager
import net.typho.big_shot_lib.api.client.event.ClientStartTickEvent
import net.typho.big_shot_lib.api.client.rendering.common.GpuQueue
import net.typho.big_shot_lib.api.event.NeoClientEventBus
import net.typho.big_shot_lib.api.util.resource.NamedResource
import net.typho.big_shot_lib.api.util.resource.SingleStepNeoReloadListener
import net.typho.vibrancy.Vibrancy
import net.typho.vibrancy.mixin.TextureAtlasAccessor
import net.typho.vibrancy.mixin.TextureAtlasSpriteAccessor
import java.io.FileNotFoundException
import java.util.concurrent.CompletableFuture

object ExtraAtlases : NamedResource, SingleStepNeoReloadListener {
    @JvmField
    val materialIdConverter = FileToIdConverter("rtx/material", ".png")
    @JvmField
    val transmissionIdConverter = FileToIdConverter("rtx/transmission", ".png")
    override val location: Identifier = Vibrancy.id("extra_atlases")
    private val reflection = hashMapOf<Identifier, TextureAtlas>()
    private val transmission = hashMapOf<Identifier, TextureAtlas>()

    override fun onResourceManagerReload(manager: ResourceManager) {
        reflection.forEach { (key, atlas) -> GpuQueue.runOrQueue { atlas.close() } }
        reflection.clear()

        transmission.forEach { (key, atlas) -> GpuQueue.runOrQueue { atlas.close() } }
        transmission.clear()
    }

    @JvmStatic
    fun onInitializeClient(bus: NeoClientEventBus) {
        bus.register(ClientStartTickEvent {
            reflection.forEach { (key, atlas) ->
                (atlas as TextureAtlasAccessor).`vibrancy$getTexturesByName`().values.forEach { (it.contents() as SpriteContentsExtension).`sodium$setActive`(true) }
                atlas.tick()
            }
            transmission.forEach { (key, atlas) ->
                (atlas as TextureAtlasAccessor).`vibrancy$getTexturesByName`().values.forEach { (it.contents() as SpriteContentsExtension).`sodium$setActive`(true) }
                atlas.tick()
            }
        })
    }

    private fun createAtlas(type: String, parentKey: Identifier, idConverter: FileToIdConverter, resources: ResourceManager, defaults: Boolean): TextureAtlas {
        val parent = try {
            Minecraft.getInstance().atlasManager.getAtlasOrThrow(parentKey)
        } catch (_: NullPointerException) {
            throw FileNotFoundException("No atlas $parentKey")
        }
        val key = Vibrancy.id(parentKey.toString('/') + "/$type")
        val loader = SpriteResourceLoader.create(setOf(AnimationMetadataSection.TYPE))
        val sprites = mutableMapOf<Identifier, TextureAtlasSprite>()

        sprites[MissingTextureAtlasSprite.getLocation()] = parent.missingSprite()

        if (defaults) {
            SpriteSourceList.load(resources, parentKey).list(resources).forEach {
                it.get(loader)?.let { contents ->
                    val parentSprite = parent.getSprite(contents.name())
                    val sprite = TextureAtlasSpriteAccessor.init(
                        contents.name(),
                        contents,
                        parent.texture.width,
                        parent.texture.height,
                        parentSprite.x,
                        parentSprite.y,
                        (parentSprite as TextureAtlasSpriteAccessor).`vibrancy$getPadding`()
                    )
                    sprites[contents.name()] = sprite
                }
            }
        }

        for ((fileId, resource) in idConverter.listMatchingResources(resources)) {
            val id = idConverter.fileToId(fileId)
            val parentSprite = parent.getSprite(id)

            loader.loadSprite(id, resource)?.let { contents ->
                val sprite = TextureAtlasSpriteAccessor.init(
                    id,
                    contents,
                    parent.texture.width,
                    parent.texture.height,
                    parentSprite.x,
                    parentSprite.y,
                    (parentSprite as TextureAtlasSpriteAccessor).`vibrancy$getPadding`()
                )
                sprites[id] = sprite
            }
        }

        val mipLevel = (parent as TextureAtlasAccessor).`vibrancy$getMaxMipLevel`()

        sprites.values.forEach { it.contents().increaseMipLevel(mipLevel) }

        val atlas = TextureAtlas(key)
        atlas.upload(SpriteLoader.Preparations(parent.texture.width, parent.texture.height, mipLevel, parent.missingSprite(), sprites, CompletableFuture.completedFuture(null)))
        return atlas
    }

    @JvmStatic
    @JvmOverloads
    fun getMaterial(key: Identifier, resources: ResourceManager = Minecraft.getInstance().resourceManager): TextureAtlas {
        return reflection.computeIfAbsent(key) { key -> createAtlas("reflection", key, materialIdConverter, resources, false) }
    }

    @JvmStatic
    @JvmOverloads
    fun getTransmission(key: Identifier, resources: ResourceManager = Minecraft.getInstance().resourceManager): TextureAtlas {
        return transmission.computeIfAbsent(key) { key -> createAtlas("transmission", key, transmissionIdConverter, resources, true) }
    }
}