package net.typho.vibrancy.util

import com.mojang.blaze3d.platform.NativeImage
import net.minecraft.client.Minecraft
import net.typho.big_shot_lib.api.client.rendering.opengl.GlQueue
import net.typho.big_shot_lib.api.client.rendering.opengl.constant.*
import net.typho.big_shot_lib.api.client.rendering.opengl.resource.impl.NeoGlFramebuffer
import net.typho.big_shot_lib.api.client.rendering.opengl.resource.impl.NeoGlTexture2D
import net.typho.big_shot_lib.api.client.rendering.opengl.resource.type.GlTexture2D
import net.typho.big_shot_lib.api.client.rendering.quad.NeoAtlas
import net.typho.big_shot_lib.api.client.util.resource.NeoResourceManager
import net.typho.big_shot_lib.api.client.util.resource.NeoResourceManagerReloadListener
import net.typho.big_shot_lib.api.math.rect.NeoRect2i
import net.typho.big_shot_lib.api.util.NeoColor
import net.typho.big_shot_lib.api.util.WrapperUtil
import net.typho.big_shot_lib.api.util.resource.NamedResource
import net.typho.big_shot_lib.api.util.resource.NeoFileToIdConverter
import net.typho.big_shot_lib.api.util.resource.NeoIdentifier
import net.typho.vibrancy.Vibrancy
import net.typho.vibrancy.mixin.NativeImageAccessor
import org.lwjgl.opengl.GL11.*
import java.io.FileNotFoundException

object ReflectionAtlases : NamedResource, NeoResourceManagerReloadListener {
    @JvmField
    val idConverter = NeoFileToIdConverter("rtx/reflections", "png")
    override val location: NeoIdentifier = Vibrancy.id("reflection_atlases")
    private val atlases = hashMapOf<NeoIdentifier, GlTexture2D>()

    override fun onResourceManagerReload(manager: NeoResourceManager) {
        atlases.forEach { (key, texture) -> GlQueue.INSTANCE.runOrQueue { texture.free() } }
        atlases.clear()
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

                NeoGlFramebuffer().bind(NeoRect2i(0, 0, parent.width, parent.height)).use {
                    it.colorAttachments[0] = texture.resource
                    it.clear(GlClearBit.Color(NeoColor.FULL_OFF))
                }

                for (resource in idConverter.listMatchingResources(resources)) {
                    val id = idConverter.fileToId(resource.key)

                    parent.sprites[id]?.let { sprite ->
                        resource.value.open().use { stream ->
                            NativeImage.read(NativeImage.Format.RGBA, stream).use { image ->
                                glTexSubImage2D(
                                    GL_TEXTURE_2D,
                                    0,
                                    sprite.x,
                                    sprite.y,
                                    image.width,
                                    image.height,
                                    GL_RGBA,
                                    GL_UNSIGNED_BYTE,
                                    (image as NativeImageAccessor).`big_shot_lib$getPixels`()
                                )
                            }
                        }
                    }
                }

                return@computeIfAbsent texture.resource
            }
        }
    }
}