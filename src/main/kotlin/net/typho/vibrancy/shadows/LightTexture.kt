package net.typho.vibrancy.shadows

import net.typho.big_shot_lib.api.client.rendering.opengl.constant.*
import net.typho.big_shot_lib.api.client.rendering.opengl.resource.impl.NeoGlFramebuffer
import net.typho.big_shot_lib.api.client.rendering.opengl.resource.impl.NeoGlTexture2D
import net.typho.big_shot_lib.api.util.NeoColor

open class LightTexture : NeoGlTexture2D() {
    companion object {
        @JvmField
        val FORMAT = GlTextureFormat.RGB8
    }

    val framebuffer = NeoGlFramebuffer()

    init {
        bind(GlTextureTarget.TEXTURE_2D).use { texture ->
            texture.textureDataMutable(1, 1, FORMAT)
            texture.minFilter = GlTextureMinFilter.NEAREST
            texture.magFilter = GlTextureMagFilter.NEAREST
        }
        framebuffer.bind().use { fbo ->
            fbo.colorAttachments[0] = this
            fbo.checkStatus().throwIfError()
            clear()
        }
    }

    override fun free() {
        super.free()
        framebuffer.free()
    }

    fun clear() {
        framebuffer.bind().use { fbo ->
            fbo.clear(GlClearBit.Color(NeoColor.FULL_ON))
        }
    }

    fun resize(width: Int, height: Int) {
        val width = width.coerceAtLeast(1)
        val height = height.coerceAtLeast(1)

        if (width != this.width || height != this.height) {
            bind(GlTextureTarget.TEXTURE_2D).use {
                it.textureDataMutable(width, height, GlTextureFormat.RGB8)
            }
        }
    }
}