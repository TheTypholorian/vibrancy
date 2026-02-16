package net.typho.vibrancy.shadows

import com.mojang.blaze3d.vertex.ByteBufferBuilder
import com.mojang.blaze3d.vertex.VertexConsumer
import com.mojang.blaze3d.vertex.VertexFormat
import com.mojang.blaze3d.vertex.VertexFormatElement
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.resources.ResourceLocation
import net.typho.big_shot_lib.api.client.rendering.buffers.BufferType
import net.typho.big_shot_lib.api.client.rendering.buffers.BufferUsage
import net.typho.big_shot_lib.api.client.rendering.buffers.GlBuffer
import net.typho.big_shot_lib.api.client.rendering.services.TextureUtil
import net.typho.big_shot_lib.api.client.rendering.shaders.GlShader
import net.typho.big_shot_lib.api.client.rendering.state.ComparisonFunc
import net.typho.big_shot_lib.api.client.rendering.state.GlFlag
import net.typho.big_shot_lib.api.client.rendering.state.OpenGL
import net.typho.big_shot_lib.api.client.rendering.textures.*
import net.typho.big_shot_lib.api.client.rendering.util.MeshUtil
import net.typho.big_shot_lib.api.util.IColor
import net.typho.big_shot_lib.api.util.resources.ResourceIdentifier
import net.typho.vibrancy.util.EmptyVertexConsumer
import org.lwjgl.system.NativeResource
import java.util.*
import java.util.function.Consumer
import kotlin.jvm.optionals.getOrNull

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

    override fun free() {
        target.free()
    }

    fun getRenderTypeTexture(renderType: RenderType): ResourceLocation? {
        return when (renderType) {
            is RenderType.CompositeRenderType -> {
                renderType.state().textureState.cutoutTexture().getOrNull()
            }
            else -> null
        }
    }

    fun begin(shader: GlShader, uniforms: Consumer<GlShader>) = Builder(shader, uniforms)

    inner class Builder(
        @JvmField
        val shader: GlShader,
        @JvmField
        val uniforms: Consumer<GlShader>
    ) : MultiBufferSource {
        val builders = HashMap<ResourceIdentifier, ShadowBufferBuilder>()

        override fun getBuffer(renderType: RenderType): VertexConsumer {
            val texture = getRenderTypeTexture(renderType)

            return if (
                texture == null
                || renderType.mode() != VertexFormat.Mode.QUADS
                || !renderType.format().contains(VertexFormatElement.POSITION)
                || !renderType.format().contains(VertexFormatElement.UV0)
            ) {
                EmptyVertexConsumer
            } else {
                builders.computeIfAbsent(ResourceIdentifier(texture.namespace, texture.path)) { // TODO
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

            GlFlag.CULL_FACE.disable()
            GlFlag.DEPTH_TEST.enable()
            OpenGL.INSTANCE.depthFunc(ComparisonFunc.ALWAYS)
            GlFlag.BLEND.disable()

            size = 0

            if (!builders.isEmpty()) {
                val mesh = MeshUtil.SCREEN_MESH
                mesh.bind()

                val ssbo = GlBuffer(BufferType.SHADER_STORAGE_BUFFER, BufferUsage.STREAM_DRAW)
                ssbo.bind()
                ssbo.bindBase(0)

                for (entry in builders) {
                    size += entry.value.numQuads()

                    shader.getUniform("Sampler0")?.setSampler(TextureUtil.INSTANCE.getMinecraftTexture(entry.key))
                    ssbo.upload(entry.value.build())

                    mesh.draw()
                }

                ssbo.unbind()
                mesh.unbind()
            }

            for (builder in toFree) {
                builder.close()
            }

            shader.unbind()
            target.unbind()

            toFree.clear()
            builders.clear()
        }
    }
}