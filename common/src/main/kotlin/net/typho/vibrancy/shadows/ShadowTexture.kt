package net.typho.vibrancy.shadows

import com.mojang.blaze3d.vertex.ByteBufferBuilder
import com.mojang.blaze3d.vertex.VertexConsumer
import com.mojang.blaze3d.vertex.VertexFormat
import com.mojang.blaze3d.vertex.VertexFormatElement
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.typho.big_shot_lib.api.client.rendering.buffers.BufferType
import net.typho.big_shot_lib.api.client.rendering.buffers.BufferUsage
import net.typho.big_shot_lib.api.client.rendering.buffers.GlBuffer
import net.typho.big_shot_lib.api.client.rendering.services.TextureUtil
import net.typho.big_shot_lib.api.client.rendering.shaders.GlShader
import net.typho.big_shot_lib.api.client.rendering.state.BlendShard
import net.typho.big_shot_lib.api.client.rendering.state.CullShard
import net.typho.big_shot_lib.api.client.rendering.state.RenderSettings
import net.typho.big_shot_lib.api.client.rendering.textures.*
import net.typho.big_shot_lib.api.client.rendering.util.MeshUtil
import net.typho.big_shot_lib.api.util.IColor
import net.typho.big_shot_lib.api.util.resources.ResourceIdentifier
import net.typho.vibrancy.Vibrancy
import net.typho.vibrancy.util.EmptyVertexConsumer
import org.lwjgl.system.NativeResource
import java.util.*
import java.util.function.Consumer

open class ShadowTexture(
    @JvmField
    val width: Int,
    @JvmField
    val height: Int
) : NativeResource {
    val texture by lazy {
        val texture = NeoTexture2D(TextureFormat.R16F)
        texture.resize(width, height)
        texture.setInterpolation(InterpolationType.LINEAR)
        return@lazy texture
    }
    val target by lazy {
        NeoFramebuffer(
            listOf(texture),
            null,
            width,
            height
        )
    }
    @JvmField
    var shadows: Collection<LightFace> = emptyList()
    @JvmField
    val toFree = LinkedList<ByteBufferBuilder>()
    @JvmField
    var size = 0
    @JvmField
    val renderSettings = RenderSettings(
        Vibrancy.id("shadow_texture"),
        listOf(
            CullShard.getDefault(),
            BlendShard.getDefault()
        )
    )

    override fun free() {
        target.free()
    }

    fun begin(shader: GlShader, uniforms: Consumer<GlShader>) = Builder(shader, uniforms)

    inner class Builder(
        @JvmField
        val shader: GlShader,
        @JvmField
        val uniforms: Consumer<GlShader>
    ) : MultiBufferSource {
        val builders = HashMap<ResourceIdentifier, ShadowBufferBuilder>()

        fun mainBuffer(): ShadowBufferBuilder {
            return builders.computeIfAbsent(TextureUtil.INSTANCE.blockAtlasTexture()) {
                val builder = ByteBufferBuilder(RenderType.SMALL_BUFFER_SIZE)
                toFree.add(builder)
                ShadowBufferBuilder(builder)
            }
        }

        override fun getBuffer(renderType: RenderType): VertexConsumer {
            val texture = TextureUtil.INSTANCE.getRenderTypeTexture(renderType)

            return if (
                texture == null
                || renderType.mode() != VertexFormat.Mode.QUADS // TODO ?
                || !renderType.format().contains(VertexFormatElement.POSITION)
                || !renderType.format().contains(VertexFormatElement.UV0)
            ) {
                EmptyVertexConsumer
            } else {
                builders.computeIfAbsent(texture) {
                    val builder = ByteBufferBuilder(renderType.bufferSize())
                    toFree.add(builder)
                    ShadowBufferBuilder(builder)
                }
            }
        }

        fun finish() {
            target.bind()

            target.viewport()
            target.clear(ClearBit.Color(IColor.BLACK))

            shader.bind()
            uniforms.accept(shader)

            renderSettings.bind()

            size = 0

            if (!builders.isEmpty()) {
                val mesh = MeshUtil.SCREEN_MESH
                mesh.bind()

                val ssbo = GlBuffer(BufferType.SHADER_STORAGE_BUFFER, BufferUsage.STREAM_DRAW)
                ssbo.bind()
                ssbo.bindBase(0)

                for (entry in builders) {
                    entry.value.build()?.let { built ->
                        size += entry.value.numQuads()

                        shader.getUniform("Sampler0")?.setSampler(TextureUtil.INSTANCE.getMinecraftTexture(entry.key))
                        ssbo.upload(built)

                        mesh.draw()
                    }
                }

                ssbo.unbind()
                mesh.unbind()
            }

            for (builder in toFree) {
                builder.close()
            }

            renderSettings.unbind()

            shader.unbind()
            target.unbind()

            toFree.clear()
            builders.clear()
        }
    }
}