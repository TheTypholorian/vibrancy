package net.typho.vibrancy.shadows

import net.typho.big_shot_lib.api.client.opengl.buffers.BufferUsage
import net.typho.big_shot_lib.api.client.opengl.buffers.Mesh
import net.typho.big_shot_lib.api.client.opengl.buffers.NeoVertexFormat
import net.typho.big_shot_lib.api.client.opengl.state.*
import net.typho.big_shot_lib.api.client.opengl.util.GlShapeType
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
    }

    var size = 0
        private set
    val mesh by lazy {
        Mesh(
            VERTEX_FORMAT,
            GlShapeType.QUADS,
            BufferUsage.STATIC_DRAW
        )
    }

    override fun free() {
        mesh.free()
    }

    fun renderStencil(): RenderSettings {
        volumeWriteSettings.bind()
        mesh.draw()
        volumeWriteSettings.unbind()

        return volumeReadSettings
    }

    inner class Builder { // : MultiBufferSource
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
            size = 0

            meshBuilder.build()?.let { built ->
                size += built.drawState().indexCount

                mesh.upload(built)
            }

            meshBuilder.buffer.close()
        }
    }
}