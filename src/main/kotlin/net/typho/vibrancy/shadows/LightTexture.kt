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

    @JvmField
    val framebuffer = NeoGlFramebuffer()

    init {
        init()
    }

    protected open fun init() {
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

    open fun clear() {
        framebuffer.bind().use { fbo ->
            fbo.clear(GlClearBit.Color(NeoColor.FULL_ON))
        }
    }

    open fun resize(width: Int, height: Int) {
        val width = width.coerceAtLeast(1)
        val height = height.coerceAtLeast(1)

        if (width != this.width || height != this.height) {
            bind(GlTextureTarget.TEXTURE_2D).use {
                it.textureDataMutable(width, height, FORMAT)
            }
        }
    }

    open class Shadow : LightTexture() {
        var depth: NeoGlTexture2D? = null
            protected set

        override fun init() {
            bind(GlTextureTarget.TEXTURE_2D).use { texture ->
                texture.textureDataMutable(1, 1, FORMAT)
                texture.minFilter = GlTextureMinFilter.NEAREST
                texture.magFilter = GlTextureMagFilter.NEAREST
            }
            val depth = NeoGlTexture2D()
            this.depth = depth
            depth.bind(GlTextureTarget.TEXTURE_2D).use { texture ->
                texture.textureDataMutable(1, 1, GlTextureFormat.DEPTH_COMPONENT32F)
                texture.minFilter = GlTextureMinFilter.NEAREST
                texture.magFilter = GlTextureMagFilter.NEAREST
            }
            framebuffer.bind().use { fbo ->
                fbo.colorAttachments[0] = this
                fbo.depthAttachment = depth
                fbo.checkStatus().throwIfError()
                clear()
            }
        }

        override fun clear() {
            framebuffer.bind().use { fbo ->
                fbo.clear(GlClearBit.Color(NeoColor.FULL_ON), GlClearBit.Depth(1f))
            }
        }

        override fun resize(width: Int, height: Int) {
            val width = width.coerceAtLeast(1)
            val height = height.coerceAtLeast(1)

            if (width != this.width || height != this.height) {
                bind(GlTextureTarget.TEXTURE_2D).use {
                    it.textureDataMutable(width, height, FORMAT)
                }
                depth!!.bind(GlTextureTarget.TEXTURE_2D).use {
                    it.textureDataMutable(width, height, GlTextureFormat.DEPTH_COMPONENT32F)
                }
            }
        }
    }
}