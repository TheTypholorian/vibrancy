package net.typho.vibrancy.shadows

import net.typho.big_shot_lib.api.client.rendering.opengl.constant.GlBeginMode
import net.typho.big_shot_lib.api.client.rendering.opengl.constant.GlBufferTarget
import net.typho.big_shot_lib.api.client.rendering.opengl.constant.GlBufferUsage
import net.typho.big_shot_lib.api.client.rendering.opengl.resource.bound.GlBufferWriter
import net.typho.big_shot_lib.api.client.rendering.opengl.resource.impl.NeoGlBuffer
import net.typho.big_shot_lib.api.client.rendering.util.NeoBufferBuilder
import net.typho.big_shot_lib.api.client.rendering.util.NeoVertexFormat
import net.typho.vibrancy.TextureAtlas
import org.lwjgl.system.NativeResource

open class ShadowMesh : NativeResource {
    companion object {
        @JvmField
        val VERTEX_FORMAT = NeoVertexFormat.builder()
            .add("Position", NeoVertexFormat.Element.POSITION)
            .padding(4)
            .add("UV0", NeoVertexFormat.Element.TEXTURE_UV)
            .add("Color", NeoVertexFormat.Element.COLOR)
            .padding(4)
            .build()
    }

    @JvmField
    val shadowBuffer = NeoGlBuffer()
    @JvmField
    val lightMesh = LightMesh()

    override fun free() {
        lightMesh.free()
    }

    fun build(
        shadowFaces: List<LightFace>,
        lightFaces: List<LightFace>
    ): () -> TextureAtlas.Result {
        val light = lightMesh.build(lightFaces)

        return {
            shadowBuffer.bind(GlBufferTarget.ARRAY_BUFFER).use { shadowBuffer ->
                if (shadowFaces.isEmpty()) {
                    shadowBuffer.bufferData(0, GlBufferUsage.STATIC_DRAW)
                } else {
                    val bufferBuilder = NeoBufferBuilder(
                        VERTEX_FORMAT,
                        GlBeginMode.QUADS,
                        shadowFaces.size * 4,
                        { GlBufferWriter.Mode.REGULAR.create(shadowBuffer, it, GlBufferUsage.STATIC_DRAW) },
                        { null }
                    )

                    shadowFaces.forEach {
                        it.quad.put(bufferBuilder)
                    }

                    bufferBuilder.build()
                }
            }

            light()
        }
    }
}