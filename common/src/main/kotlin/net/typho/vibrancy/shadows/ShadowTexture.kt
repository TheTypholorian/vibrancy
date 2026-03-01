package net.typho.vibrancy.shadows

import net.typho.big_shot_lib.api.client.opengl.buffers.*
import net.typho.big_shot_lib.api.client.opengl.state.*
import net.typho.big_shot_lib.api.client.opengl.util.*
import net.typho.big_shot_lib.api.util.IColor
import net.typho.vibrancy.Vibrancy
import org.lwjgl.system.NativeResource

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
                        ComparisonFunc.NOTEQUAL,
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
                        ComparisonFunc.EQUAL,
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

        @JvmStatic
        fun builderSettings(shader: ShaderShard, target: GlFramebuffer) = RenderSettings(
            Vibrancy.id("shadow_texture_builder"),
            listOf(
                DisableFlagsShard(listOf(
                    GlFlag.CULL_FACE,
                    GlFlag.BLEND
                )),
                FramebufferShard(
                    { target },
                    true,
                    ClearBit.Color(IColor.FULL_OFF)
                ),
                shader
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
        texture.free()
        mesh.free()
    }

    fun renderStencil(): RenderSettings {
        volumeWriteSettings.bind()
        mesh.draw()
        volumeWriteSettings.unbind()

        return volumeReadSettings
    }

    inner class Builder(
        shader: ShaderShard
    ) { // : MultiBufferSource
        @JvmField
        val meshBuilder = mesh.Builder()
        @JvmField
        val builderSettings = builderSettings(shader, target)

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
            builderSettings.bind()

            size = 0

            meshBuilder.build()?.let { built ->
                size += built.drawState().indexCount

                mesh.upload(built)
                meshBuilder.buffer.close()

                OpenGL.INSTANCE.bindBufferBase(BufferType.SHADER_STORAGE_BUFFER, 0, mesh.vbo.glId)

                MeshUtil.SCREEN_MESH.draw()
            }

            builderSettings.unbind()
        }
    }
}