package net.typho.vibrancy.shadows

import net.typho.big_shot_lib.api.client.rendering.opengl.constant.GlBeginMode
import net.typho.big_shot_lib.api.client.rendering.opengl.constant.GlBufferUsage
import net.typho.big_shot_lib.api.client.rendering.opengl.constant.GlIndexDataType
import net.typho.big_shot_lib.api.client.rendering.opengl.resource.bound.GlBufferWriter
import net.typho.big_shot_lib.api.client.rendering.util.Mesh
import net.typho.big_shot_lib.api.client.rendering.util.NeoVertexFormat
import net.typho.big_shot_lib.api.util.buffer.BYTE_MASK
import net.typho.big_shot_lib.api.util.buffer.NeoBuffer
import net.typho.big_shot_lib.api.util.buffer.SHORT_MASK
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
    val shadowMesh = Mesh(
        VERTEX_FORMAT,
        GlBeginMode.QUADS,
        GlBufferWriter.Mode.REGULAR,
        GlBufferUsage.STATIC_DRAW
    )
    @JvmField
    val lightMesh = LightMesh(GlBufferUsage.STATIC_DRAW)

    override fun free() {
        lightMesh.free()
    }

    fun drawDebug() {
        shadowMesh.draw()
    }

    fun build(
        shadowFaces: List<LightFace>,
        lightFaces: List<LightFace>
    ): () -> TextureAtlas.Result {
        val light = lightMesh.build(lightFaces)

        if (shadowFaces.isEmpty()) {
            return {
                shadowMesh.builder(0)?.build()
                light()
            }
        } else {
            val vertexBuffer = NeoBuffer.Native(shadowFaces.size.toLong() * 4 * VERTEX_FORMAT.vertexSizeBytes)
            val indexCount = shadowFaces.size * 6
            val indexType = when (indexCount) {
                indexCount and BYTE_MASK -> GlIndexDataType.BYTE
                indexCount and SHORT_MASK -> GlIndexDataType.SHORT
                else -> GlIndexDataType.INT
            }
            val indexBuffer = NeoBuffer.Native(indexCount.toLong() * VERTEX_FORMAT.vertexSizeBytes)

            vertexBuffer.write().run {
                shadowFaces.forEachIndexed { index, face ->
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
            indexBuffer.write().run {
                var vertex = 0

                repeat(shadowFaces.size) {
                    indexType.write(this, vertex)
                    indexType.write(this, vertex + 1)
                    indexType.write(this, vertex + 2)
                    indexType.write(this, vertex + 2)
                    indexType.write(this, vertex + 3)
                    indexType.write(this, vertex)
                    vertex += 4
                }
            }

            return {
                shadowMesh.rawUpload(indexCount, indexType, vertexBuffer, indexBuffer)
                vertexBuffer.free()
                indexBuffer.free()
                light()
            }
        }
    }
}