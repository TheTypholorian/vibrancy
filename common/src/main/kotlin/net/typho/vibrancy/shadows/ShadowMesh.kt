package net.typho.vibrancy.shadows

import net.typho.big_shot_lib.api.client.rendering.opengl.constant.GlBufferTarget
import net.typho.big_shot_lib.api.client.rendering.opengl.constant.GlBufferUsage
import net.typho.big_shot_lib.api.client.rendering.opengl.resource.impl.NeoGlBuffer
import net.typho.big_shot_lib.api.client.rendering.util.NeoVertexFormat
import net.typho.big_shot_lib.api.util.buffer.NeoBuffer
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

        if (shadowFaces.isEmpty()) {
            return {
                shadowBuffer.bind(GlBufferTarget.ARRAY_BUFFER).use { it.bufferData(0, GlBufferUsage.STATIC_DRAW) }
                light()
            }
        } else {
            val buffer = NeoBuffer.Native(shadowFaces.size.toLong() * 4 * VERTEX_FORMAT.vertexSizeBytes)

            buffer.write().run {
                for (face in shadowFaces) {
                    for (vertex in face.quad.vertices) {
                        writeFloat(vertex.pos.x)
                        writeFloat(vertex.pos.y)
                        writeFloat(vertex.pos.z)
                        writeInt(0)

                        writeFloat(vertex.textureUV!!.x)
                        writeFloat(vertex.textureUV!!.y)
                        writeInt(vertex.color!!.toRGBA())
                        writeInt(0)
                    }
                }
            }

            return {
                shadowBuffer.bind(GlBufferTarget.ARRAY_BUFFER).use {
                    it.bufferData(buffer, GlBufferUsage.STATIC_DRAW)
                    buffer.free()
                }
                light()
            }
        }
    }
}