package net.typho.vibrancy.shadows

import net.typho.big_shot_lib.api.client.rendering.opengl.constant.*
import net.typho.big_shot_lib.api.client.rendering.opengl.resource.impl.NeoGlFramebuffer
import net.typho.big_shot_lib.api.client.rendering.opengl.resource.impl.NeoGlTexture2D
import net.typho.big_shot_lib.api.util.NeoColor
import org.lwjgl.opengl.GL11.glClearDepth

// TODO
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

        if (width > (this.width ?: 0) || height > (this.height ?: 0)) {
            bind(GlTextureTarget.TEXTURE_2D).use {
                it.textureDataMutable(width, height, FORMAT)
            }
        }
    }

    open class ColorShadow : LightTexture() {
        @JvmField
        val depth = NeoGlTexture2D()

        override fun init() {
        }

        init {
            bind(GlTextureTarget.TEXTURE_2D).use { texture ->
                texture.textureDataMutable(1, 1, FORMAT)
                texture.minFilter = GlTextureMinFilter.NEAREST
                texture.magFilter = GlTextureMagFilter.NEAREST
                texture.borderColor = NeoColor.FULL_OFF
                texture.wrapS = GlTextureWrapMode.CLAMP_TO_BORDER
                texture.wrapT = GlTextureWrapMode.CLAMP_TO_BORDER
            }
            depth.bind(GlTextureTarget.TEXTURE_2D).use { texture ->
                texture.textureDataMutable(1, 1, GlTextureFormat.DEPTH_COMPONENT32F)
                texture.minFilter = GlTextureMinFilter.LINEAR
                texture.magFilter = GlTextureMagFilter.LINEAR
                texture.compareMode = GlTextureCompareMode.COMPARE_REF_TO_TEXTURE
                texture.compareFunc = GlAlphaFunction.GEQUAL
                texture.borderColor = NeoColor.FULL_OFF
                texture.wrapS = GlTextureWrapMode.CLAMP_TO_BORDER
                texture.wrapT = GlTextureWrapMode.CLAMP_TO_BORDER
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
                fbo.clear(GlClearBit.Color(NeoColor.FULL_ON), GlClearBit.Depth(0f))
                glClearDepth(1.0)
            }
        }

        override fun resize(width: Int, height: Int) {
            val width = width.coerceAtLeast(1)
            val height = height.coerceAtLeast(1)

            if (width != this.width || height != this.height) {
                bind(GlTextureTarget.TEXTURE_2D).use {
                    it.textureDataMutable(width, height, FORMAT)
                }
                depth.bind(GlTextureTarget.TEXTURE_2D).use {
                    it.textureDataMutable(width, height, GlTextureFormat.DEPTH_COMPONENT32F)
                }
            }
        }
    }

    open class Shadow : LightTexture() {
        override fun init() {
            bind(GlTextureTarget.TEXTURE_2D).use { texture ->
                texture.textureDataMutable(1, 1, GlTextureFormat.DEPTH_COMPONENT32F)
                texture.minFilter = GlTextureMinFilter.LINEAR
                texture.magFilter = GlTextureMagFilter.LINEAR
                texture.compareMode = GlTextureCompareMode.COMPARE_REF_TO_TEXTURE
                texture.compareFunc = GlAlphaFunction.GEQUAL
                texture.borderColor = NeoColor.FULL_OFF
                texture.wrapS = GlTextureWrapMode.CLAMP_TO_BORDER
                texture.wrapT = GlTextureWrapMode.CLAMP_TO_BORDER
            }
            framebuffer.bind().use { fbo ->
                fbo.depthAttachment = this
                fbo.checkStatus().throwIfError()
                clear()
            }
        }

        override fun clear() {
            framebuffer.bind().use { fbo ->
                fbo.clear(GlClearBit.Color(NeoColor.FULL_ON), GlClearBit.Depth(0f))
                glClearDepth(1.0)
            }
        }

        override fun resize(width: Int, height: Int) {
            val width = width.coerceAtLeast(1)
            val height = height.coerceAtLeast(1)

            if (width != this.width || height != this.height) {
                bind(GlTextureTarget.TEXTURE_2D).use {
                    it.textureDataMutable(width, height, GlTextureFormat.DEPTH_COMPONENT32F)
                }
            }
        }
    }
}