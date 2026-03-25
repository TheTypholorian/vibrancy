package net.typho.vibrancy.util

import com.mojang.blaze3d.platform.NativeImage
import net.minecraft.client.Minecraft
import net.typho.big_shot_lib.api.client.opengl.buffers.ClearBit
import net.typho.big_shot_lib.api.client.opengl.buffers.NeoFramebuffer
import net.typho.big_shot_lib.api.client.opengl.buffers.NeoTexture2D
import net.typho.big_shot_lib.api.client.opengl.util.OpenGL
import net.typho.big_shot_lib.api.client.opengl.util.TextureFormat
import net.typho.big_shot_lib.api.client.opengl.util.TextureUtil
import net.typho.big_shot_lib.api.util.IColor
import net.typho.big_shot_lib.api.util.WrapperUtil
import net.typho.big_shot_lib.api.util.resources.*
import net.typho.vibrancy.Vibrancy
import net.typho.vibrancy.mixin.NativeImageAccessor
import org.lwjgl.opengl.GL11.*

object ReflectionAtlases : NamedResource, NeoResourceManagerReloadListener {
    @JvmField
    val idConverter = NeoFileToIdConverter("rtx/reflections", "png")
    override val location: ResourceIdentifier = Vibrancy.id("reflection_atlases")
    private val atlases = hashMapOf<ResourceIdentifier, NeoTexture2D>()

    override fun onResourceManagerReload(manager: NeoResourceManager) {
        atlases.forEach { (key, texture) -> OpenGL.INSTANCE.recordRenderCall { texture.free() } }
        atlases.clear()
    }

    operator fun get(key: ResourceIdentifier, resources: NeoResourceManager = WrapperUtil.INSTANCE.wrap(Minecraft.getInstance().resourceManager)): NeoTexture2D {
        return atlases.computeIfAbsent(key) { key ->
            val parent = TextureUtil.INSTANCE.getAtlas(key)
            val texture = NeoTexture2D(TextureFormat.R8)

            NeoFramebuffer(
                listOf(texture),
                null,
                parent.width,
                parent.height
            ).use {
                it.clear(ClearBit.Color(IColor.FULL_OFF))
            }

            texture.bind()

            for (resource in idConverter.listMatchingResources(resources)) {
                val id = idConverter.fileToId(resource.key)

                parent.sprites[id]?.let { sprite ->
                    resource.value.open().use { stream ->
                        NativeImage.read(NativeImage.Format.RGBA, stream).use { image ->
                            glTexSubImage2D(GL_TEXTURE_2D, 0, sprite.x, sprite.y, image.width, image.height, GL_RGBA, GL_UNSIGNED_BYTE, (image as NativeImageAccessor).`big_shot_lib$getPixels`())
                        }
                    }
                }
            }

            texture.unbind()

            return@computeIfAbsent texture
        }
    }
}