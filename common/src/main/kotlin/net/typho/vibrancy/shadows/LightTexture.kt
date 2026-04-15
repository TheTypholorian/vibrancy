package net.typho.vibrancy.shadows

import net.typho.big_shot_lib.api.client.rendering.opengl.constant.GlTextureFormat
import net.typho.big_shot_lib.api.client.rendering.opengl.constant.GlTextureMagFilter
import net.typho.big_shot_lib.api.client.rendering.opengl.constant.GlTextureMinFilter
import net.typho.big_shot_lib.api.client.rendering.opengl.constant.GlTextureTarget
import net.typho.big_shot_lib.api.client.rendering.opengl.resource.impl.NeoGlTexture2D

open class LightTexture : NeoGlTexture2D() {
    companion object {
        @JvmField
        val FORMAT = GlTextureFormat.RGB8
    }

    init {
        bind(GlTextureTarget.TEXTURE_2D).use { texture ->
            texture.textureDataMutable(1, 1, FORMAT)
            texture.minFilter = GlTextureMinFilter.NEAREST
            texture.magFilter = GlTextureMagFilter.NEAREST
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