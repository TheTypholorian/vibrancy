package net.typho.vibrancy.shadows

import net.typho.big_shot_lib.api.client.rendering.opengl.constant.GlBufferTarget
import net.typho.big_shot_lib.api.client.rendering.opengl.constant.GlBufferUsage
import net.typho.big_shot_lib.api.client.rendering.opengl.resource.impl.NeoGlBuffer
import net.typho.big_shot_lib.api.client.rendering.util.NeoVertexFormat
import net.typho.big_shot_lib.api.math.vec.AbstractVec3.Companion.toJOML
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
    var numShadows: Int = 0
        protected set
    @JvmField
    val lightMesh = LightMesh(GlBufferUsage.STATIC_DRAW)

    override fun free() {
        shadowBuffer.free()
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
                numShadows = 0
                light()
            }
        } else {
            val buffer = NeoBuffer.Native(shadowFaces.size.toLong() * (4 * VERTEX_FORMAT.vertexSizeBytes + 8 * Float.SIZE_BYTES))

            buffer.write().run {
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

                    writeFloat(face.quad.v0.normal!!.x)
                    writeFloat(face.quad.v0.normal!!.y)
                    writeFloat(face.quad.v0.normal!!.z)
                    writeInt(0)

                    writeFloat(face.quad.v0.normal!!.toJOML().dot(face.quad.v0.pos.toJOML()))

                    val diagonal1 = face.quad.v1.pos - face.quad.v0.pos
                    val diagonal2 = face.quad.v3.pos - face.quad.v0.pos

                    val d11 = diagonal1.toJOML().dot(diagonal1.toJOML())
                    val d12 = diagonal1.toJOML().dot(diagonal2.toJOML())
                    val d22 = diagonal2.toJOML().dot(diagonal2.toJOML())
                    val invDet = 1 / (d11 * d22 - d12 * d12)

                    writeFloat(d22 * invDet)
                    writeFloat(-d12 * invDet)
                    writeFloat(d11 * invDet)
                }
            }

            return {
                shadowBuffer.bind(GlBufferTarget.ARRAY_BUFFER).use { it.bufferData(buffer, GlBufferUsage.STATIC_DRAW) }
                buffer.free()
                numShadows = shadowFaces.size
                light()
            }
        }
    }
}