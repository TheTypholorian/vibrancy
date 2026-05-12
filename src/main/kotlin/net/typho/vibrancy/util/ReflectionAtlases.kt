package net.typho.vibrancy.util

import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.texture.SpriteContents
import net.minecraft.client.renderer.texture.SpriteLoader
import net.minecraft.client.renderer.texture.SpriteTicker
import net.minecraft.client.renderer.texture.atlas.SpriteResourceLoader
import net.minecraft.resources.ResourceLocation
import net.typho.big_shot_lib.api.client.rendering.opengl.GlQueue
import net.typho.big_shot_lib.api.client.rendering.opengl.constant.*
import net.typho.big_shot_lib.api.client.rendering.opengl.resource.impl.NeoGlFramebuffer
import net.typho.big_shot_lib.api.client.rendering.opengl.resource.impl.NeoGlTexture2D
import net.typho.big_shot_lib.api.client.rendering.opengl.resource.type.GlTexture2D
import net.typho.big_shot_lib.api.client.rendering.util.NeoAtlas
import net.typho.big_shot_lib.api.client.util.BigShotClientEntrypoint
import net.typho.big_shot_lib.api.client.util.event.ClientEventFactory
import net.typho.big_shot_lib.api.client.util.resource.NeoResourceManager
import net.typho.big_shot_lib.api.client.util.resource.NeoResourceManagerReloadListener
import net.typho.big_shot_lib.api.math.rect.NeoRect2i
import net.typho.big_shot_lib.api.util.NeoColor
import net.typho.big_shot_lib.api.util.WrapperUtil
import net.typho.big_shot_lib.api.util.resource.NamedResource
import net.typho.big_shot_lib.api.util.resource.NeoFileToIdConverter
import net.typho.big_shot_lib.api.util.resource.NeoIdentifier
import net.typho.vibrancy.Vibrancy
import java.io.FileNotFoundException

//? if >=1.21.5 {
import net.typho.vibrancy.mixin.GlTextureAccessor
import com.mojang.blaze3d.textures.TextureFormat
//? }

object ReflectionAtlases : NamedResource, NeoResourceManagerReloadListener, BigShotClientEntrypoint {
    private class Animation(
        @JvmField
        val x: Int,
        @JvmField
        val y: Int,
        @JvmField
        val contents: SpriteContents,
        @JvmField
        val ticker: SpriteTicker
    )

    private class Atlas(
        @JvmField
        val texture: GlTexture2D,
        @JvmField
        val animations: List<Animation>
    )

    @JvmField
    val idConverter = NeoFileToIdConverter("rtx/reflections", "png")
    override val location: NeoIdentifier = Vibrancy.id("reflection_atlases")
    private val atlases = hashMapOf<NeoIdentifier, Atlas>()

    override fun onResourceManagerReload(manager: NeoResourceManager) {
        atlases.forEach { (key, atlas) -> GlQueue.INSTANCE.runOrQueue {
            atlas.texture.free()

            for (animation in atlas.animations) {
                animation.contents.close()
                animation.ticker.close()
            }
        } }
        atlases.clear()
    }

    override fun registerEvents(factory: ClientEventFactory) {
        factory.clientTickStart.add {
            for ((key, atlas) in atlases) {
                atlas.texture.bind(GlTextureTarget.TEXTURE_2D).use { texture ->
                    for (animation in atlas.animations) {
                        //? if <1.21.5 {
                        /*animation.ticker.tickAndUpload(animation.x, animation.y)
                        *///? } else {
                        animation.ticker.tickAndUpload(animation.x, animation.y, GlTextureAccessor.`vibrancy$init`(
                            "Vibrancy Reflection Atlas $key",
                            TextureFormat.RGBA8,
                            atlas.texture.width!!,
                            atlas.texture.height!!,
                            1,
                            atlas.texture.glId
                        ))
                        //? }
                    }
                }
            }
        }
    }

    operator fun get(key: NeoIdentifier, resources: NeoResourceManager = WrapperUtil.INSTANCE.wrap(Minecraft.getInstance().resourceManager)): GlTexture2D {
        return atlases.computeIfAbsent(key) { key ->
            NeoGlTexture2D().bind(GlTextureTarget.TEXTURE_2D).use { texture ->
                val parent = try {
                    NeoAtlas[key]!!
                } catch (_: NullPointerException) {
                    throw FileNotFoundException("No atlas $key")
                }

                texture.textureDataImmutable(parent.width, parent.height, GlTextureFormat.R8)
                texture.minFilter = GlTextureMinFilter.NEAREST
                texture.magFilter = GlTextureMagFilter.NEAREST

                NeoGlFramebuffer().use { fbo ->
                    fbo.bind(NeoRect2i(0, 0, parent.width, parent.height)).use { fbo ->
                        fbo.colorAttachments[0] = texture.resource
                        fbo.checkStatus().throwIfError()
                        fbo.clear(GlClearBit.Color(NeoColor.FULL_OFF))
                    }
                }

                val animations = mutableListOf<Animation>()

                //? if <1.21 {
                /*for (resource in idConverter.listMatchingResources(resources)) {
                    val id = idConverter.fileToId(resource.key)

                    parent.sprites[id]?.let { sprite ->
                        resource.value.open().use { stream ->
                            SpriteLoader.loadSprite(ResourceLocation(resource.key.namespace, resource.key.path), resource.value)?.let { contents ->
                                contents.uploadFirstFrame(sprite.x, sprite.y)
                                val ticker = contents.createTicker()

                                if (ticker == null) {
                                    contents.close()
                                } else {
                                    animations.add(Animation(
                                        sprite.x,
                                        sprite.y,
                                        contents,
                                        ticker
                                    ))
                                }
                            }
                        }
                    }
                }
                *///? } else {
                val loader = SpriteResourceLoader.create(SpriteLoader.DEFAULT_METADATA_SECTIONS)

                for (resource in idConverter.listMatchingResources(resources)) {
                    val id = idConverter.fileToId(resource.key)

                    parent.sprites[id]?.let { sprite ->
                        resource.value.open().use { stream ->
                            loader.loadSprite(ResourceLocation.fromNamespaceAndPath(resource.key.namespace, resource.key.path), resource.value)?.let { contents ->
                                //? if <1.21.5 {
                                /*contents.uploadFirstFrame(sprite.x, sprite.y)
                                *///? } else {
                                contents.uploadFirstFrame(sprite.x, sprite.y, GlTextureAccessor.`vibrancy$init`(
                                    "Vibrancy Reflection Atlas $key",
                                    TextureFormat.RGBA8,
                                    parent.width,
                                    parent.height,
                                    1,
                                    parent.glId
                                ))
                                //? }
                                val ticker = contents.createTicker()

                                if (ticker == null) {
                                    contents.close()
                                } else {
                                    animations.add(Animation(
                                        sprite.x,
                                        sprite.y,
                                        contents,
                                        ticker
                                    ))
                                }
                            }
                        }
                    }
                }
                //? }

                return@computeIfAbsent Atlas(texture.resource, animations)
            }
        }.texture
    }
}