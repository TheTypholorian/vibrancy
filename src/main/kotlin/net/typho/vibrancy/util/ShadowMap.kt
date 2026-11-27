package net.typho.vibrancy.util

import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.textures.GpuTexture
import com.mojang.blaze3d.textures.GpuTextureView
import com.mojang.blaze3d.textures.TextureFormat

class ShadowMap(
    @JvmField
    val width: Int,
    @JvmField
    val height: Int,
    @JvmField
    val name: String
) : AutoCloseable {
    @JvmField
    val solidDepth = RenderSystem.getDevice().createTexture(
        { "$name (solid)" },
        GpuTexture.USAGE_RENDER_ATTACHMENT or GpuTexture.USAGE_TEXTURE_BINDING,
        TextureFormat.DEPTH32,
        width,
        height,
        1,
        1
    )
    @JvmField
    val solidDepthView = RenderSystem.getDevice().createTextureView(solidDepth)
    var translucentLayer: TranslucentTexture? = null
        set(value) {
            field?.close()
            field = value
        }

    fun translucentLayerOrNew() = translucentLayer ?: TranslucentTexture().also { translucentLayer = it }

    override fun close() {
        solidDepthView.close()
        solidDepth.close()
        translucentLayer?.close()
    }

    inner class TranslucentTexture(
        @JvmField
        val color: GpuTexture = RenderSystem.getDevice().createTexture(
            { "$name (translucent color)" },
            GpuTexture.USAGE_RENDER_ATTACHMENT or GpuTexture.USAGE_TEXTURE_BINDING,
            TextureFormat.RGBA8,
            width, // TODO do these need to be full size?
            height,
            1,
            1
        ),
        @JvmField
        val depth: GpuTexture = RenderSystem.getDevice().createTexture(
            { "$name (translucent depth)" },
            GpuTexture.USAGE_RENDER_ATTACHMENT or GpuTexture.USAGE_TEXTURE_BINDING,
            TextureFormat.DEPTH32,
            width,
            height,
            1,
            1
        ),
        @JvmField
        val colorView: GpuTextureView = RenderSystem.getDevice().createTextureView(color),
        @JvmField
        val depthView: GpuTextureView = RenderSystem.getDevice().createTextureView(depth)
    ) : AutoCloseable {
        override fun close() {
            colorView.close()
            depthView.close()
            color.close()
            depth.close()
        }
    }
}