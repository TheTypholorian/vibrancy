package net.typho.vibrancy.util

import net.caffeinemc.mods.sodium.client.render.texture.SpriteContentsExtension
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.texture.AbstractTexture
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite
import net.minecraft.client.renderer.texture.SpriteLoader
import net.minecraft.client.renderer.texture.TextureAtlas
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.client.renderer.texture.TickableTexture
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
import java.util.concurrent.CompletableFuture

object TextureLayers : NamedResource, SingleStepNeoReloadListener {
    @JvmField
    val materialIdConverter = FileToIdConverter("rtx/material", ".png")
    @JvmField
    val transmissionIdConverter = FileToIdConverter("rtx/transmission", ".png")
    override val location: Identifier = Vibrancy.id("extra_atlases")
    private val reflection = hashMapOf<Identifier, AbstractTexture?>()
    private val transmission = hashMapOf<Identifier, AbstractTexture?>()

    override fun onResourceManagerReload(manager: ResourceManager) {
        reflection.forEach { (key, texture) -> GpuQueue.runOrQueue { texture?.close() } }
        reflection.clear()

        transmission.forEach { (key, texture) -> GpuQueue.runOrQueue { texture?.close() } }
        transmission.clear()
    }

    @JvmStatic
    fun onInitializeClient(bus: NeoClientEventBus) {
        bus.register(ClientStartTickEvent {
            reflection.forEach { (key, texture) ->
                if (texture is TextureAtlas) {
                    (texture as TextureAtlasAccessor).`vibrancy$getTexturesByName`().values.forEach { (it.contents() as SpriteContentsExtension).`sodium$setActive`(true) }
                }

                if (texture is TickableTexture) {
                    texture.tick()
                }
            }
            transmission.forEach { (key, texture) ->
                if (texture is TextureAtlas) {
                    (texture as TextureAtlasAccessor).`vibrancy$getTexturesByName`().values.forEach { (it.contents() as SpriteContentsExtension).`sodium$setActive`(true) }
                }

                if (texture is TickableTexture) {
                    texture.tick()
                }
            }
        })
    }

    private fun createAtlas(type: String, parentKey: Identifier, idConverter: FileToIdConverter, resources: ResourceManager, defaults: Boolean): TextureAtlas? {
        val parent = Minecraft.getInstance().atlasManager.atlasByTexture[parentKey] ?: return null
        val parentId = Minecraft.getInstance().atlasManager.atlasById.entries.first { it.value === parent }.key
        val key = Vibrancy.id("$type/${parentId.toString('/')}")
        val loader = SpriteResourceLoader.create(setOf(AnimationMetadataSection.TYPE))
        val sprites = mutableMapOf<Identifier, TextureAtlasSprite>()

        sprites[MissingTextureAtlasSprite.getLocation()] = parent.atlas.missingSprite()

        if (defaults) {
            SpriteSourceList.load(resources, parentId).list(resources).forEach {
                it.get(loader)?.let { contents ->
                    val parentSprite = parent.atlas.getSprite(contents.name())
                    val sprite = TextureAtlasSpriteAccessor.init(
                        contents.name(),
                        contents,
                        parent.atlas.texture.width,
                        parent.atlas.texture.height,
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
            val parentSprite = parent.atlas.getSprite(id)

            loader.loadSprite(id, resource)?.let { contents ->
                val sprite = TextureAtlasSpriteAccessor.init(
                    id,
                    contents,
                    parent.atlas.texture.width,
                    parent.atlas.texture.height,
                    parentSprite.x,
                    parentSprite.y,
                    (parentSprite as TextureAtlasSpriteAccessor).`vibrancy$getPadding`()
                )
                sprites[id] = sprite
            }
        }

        val mipLevel = (parent.atlas as TextureAtlasAccessor).`vibrancy$getMaxMipLevel`()

        sprites.values.forEach { it.contents().increaseMipLevel(mipLevel) }

        val atlas = TextureAtlas(key)
        atlas.upload(SpriteLoader.Preparations(parent.atlas.texture.width, parent.atlas.texture.height, mipLevel, parent.atlas.missingSprite(), sprites, CompletableFuture.completedFuture(null)))
        return atlas
    }

    @JvmStatic
    @JvmOverloads
    fun getMaterial(key: Identifier, resources: ResourceManager = Minecraft.getInstance().resourceManager): AbstractTexture? {
        return reflection.computeIfAbsent(key) { key -> createAtlas("reflection", key, materialIdConverter, resources, false) }
    }

    @JvmStatic
    @JvmOverloads
    fun getMaterialOrThrow(key: Identifier, resources: ResourceManager = Minecraft.getInstance().resourceManager): AbstractTexture {
        return getMaterial(key, resources) ?: throw NullPointerException("No Vibrancy material layer for texture $key")
    }

    @JvmStatic
    @JvmOverloads
    fun getTransmission(key: Identifier, resources: ResourceManager = Minecraft.getInstance().resourceManager): AbstractTexture? {
        return transmission.computeIfAbsent(key) { key -> createAtlas("transmission", key, transmissionIdConverter, resources, true) }
    }

    @JvmStatic
    @JvmOverloads
    fun getTransmissionOrThrow(key: Identifier, resources: ResourceManager = Minecraft.getInstance().resourceManager): AbstractTexture {
        return getTransmission(key, resources) ?: throw NullPointerException("No Vibrancy transmission layer for texture $key")
    }
}