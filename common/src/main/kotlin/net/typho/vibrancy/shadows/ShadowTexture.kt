package net.typho.vibrancy.shadows

import com.mojang.blaze3d.vertex.ByteBufferBuilder
import net.typho.big_shot_lib.api.client.opengl.buffers.*
import net.typho.big_shot_lib.api.client.opengl.shaders.GlShader
import net.typho.big_shot_lib.api.client.opengl.state.*
import net.typho.big_shot_lib.api.client.opengl.util.*
import net.typho.big_shot_lib.api.util.IColor
import net.typho.vibrancy.Vibrancy
import org.lwjgl.system.NativeResource
import java.util.*
import java.util.function.Consumer

open class ShadowTexture(
    @JvmField
    val width: Int,
    @JvmField
    val height: Int
) : NativeResource {
    companion object {
        @JvmField
        val VERTEX_FORMAT = NeoVertexFormat.builder()
            .add("Position", NeoVertexFormat.Element.POSITION)
            .padding(Float.SIZE_BYTES)
            .add("UV0", NeoVertexFormat.Element.TEXTURE_UV)
            .padding(2 * Float.SIZE_BYTES)
            .build()
        @JvmField
        val textureRenderSettings = RenderSettings(
            Vibrancy.id("shadow_texture"),
            listOf(
                DisableFlagsShard(listOf(
                    GlFlag.CULL_FACE,
                    GlFlag.BLEND
                ))
            )
        )
        @JvmField
        val volumeWriteSettings = RenderSettings(
            Vibrancy.id("shadow_volume_write"),
            listOf(
                ColorMaskShard(ColorMask(false, false, false, false)),
                DisableFlagsShard(listOf(
                    GlFlag.DEPTH_TEST
                )),
                CullShard(
                    true,
                    CullFace.FRONT
                ),
                StencilShard(
                    true,
                    StencilFunc(
                        ComparisonFunc.ALWAYS,
                        1,
                        1
                    ),
                    1,
                    StencilOp(
                        IntAction.KEEP,
                        IntAction.KEEP,
                        IntAction.REPLACE
                    )
                )
            )
        )
        @JvmField
        val volumeReadSettings = RenderSettings(
            Vibrancy.id("shadow_volume_read"),
            listOf(
                StencilShard(
                    true,
                    StencilFunc(
                        ComparisonFunc.ALWAYS,
                        0,
                        1
                    ),
                    1,
                    StencilOp(
                        IntAction.KEEP,
                        IntAction.KEEP,
                        IntAction.KEEP
                    )
                )
            )
        )
    }

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
    val mesh by lazy {
        Mesh(
            VERTEX_FORMAT,
            GlShapeType.QUADS,
            BufferUsage.STATIC_DRAW
        )
    }

    override fun free() {
        target.free()
    }

    fun renderStencil(shader: GlShader, uniforms: Consumer<GlShader>): RenderSettings {
        shader.bind()
        uniforms.accept(shader)

        volumeWriteSettings.bind()

        mesh.draw()

        volumeWriteSettings.unbind()

        shader.unbind()

        return volumeReadSettings
    }

    fun begin(shader: GlShader, uniforms: Consumer<GlShader>) = Builder(shader, uniforms)

    inner class Builder(
        @JvmField
        val shader: GlShader,
        @JvmField
        val uniforms: Consumer<GlShader>
    ) { // : MultiBufferSource
        @JvmField
        val meshBuilder = mesh.Builder()

        /*
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
         */

        fun finish() {
            target.bind()

            target.viewport()
            target.clear(ClearBit.Color(IColor.FULL_OFF))

            shader.bind()
            uniforms.accept(shader)

            textureRenderSettings.bind()

            size = 0

            meshBuilder.build()?.let { built ->
                size += built.drawState().indexCount

                mesh.upload(built)
                meshBuilder.buffer.close()

                shader.getUniform("Sampler0")?.setSampler(TextureUtil.INSTANCE.getMinecraftTexture(TextureUtil.INSTANCE.blockAtlasTexture))
                OpenGL.INSTANCE.bindBufferBase(BufferType.SHADER_STORAGE_BUFFER, 0, mesh.vbo.glId)

                MeshUtil.SCREEN_MESH.draw()
            }

            textureRenderSettings.unbind()

            shader.unbind()
            target.unbind()

            toFree.forEach { it.close() }
            toFree.clear()
        }
    }
}