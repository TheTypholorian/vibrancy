package net.typho.vibrancy.shadows

import net.typho.big_shot_lib.api.client.rendering.opengl.constant.*
import net.typho.big_shot_lib.api.client.rendering.opengl.resource.bound.GlBufferWriter
import net.typho.big_shot_lib.api.client.rendering.opengl.resource.type.GlTexture2D
import net.typho.big_shot_lib.api.client.rendering.opengl.state.*
import net.typho.big_shot_lib.api.client.rendering.opengl.util.BlendFunction
import net.typho.big_shot_lib.api.client.rendering.opengl.util.PolygonOffset
import net.typho.big_shot_lib.api.client.rendering.util.Mesh
import net.typho.big_shot_lib.api.client.rendering.util.NeoVertexFormat
import net.typho.big_shot_lib.api.math.vec.IVec3
import net.typho.big_shot_lib.api.math.vec.NeoVec2i
import net.typho.big_shot_lib.api.util.buffer.BYTE_MASK
import net.typho.big_shot_lib.api.util.buffer.NeoBuffer
import net.typho.big_shot_lib.api.util.buffer.SHORT_MASK
import net.typho.big_shot_lib.api.util.resource.NeoIdentifier
import net.typho.vibrancy.TextureAtlas
import net.typho.vibrancy.VibrancyConfig
import org.lwjgl.system.NativeResource

open class LightMesh(
    @JvmField
    val usage: GlBufferUsage
) : NativeResource {
    companion object {
        @JvmField
        val VERTEX_FORMAT = NeoVertexFormat.builder()
            .add("Position", NeoVertexFormat.Element.POSITION)
            .add("UV0", NeoVertexFormat.Element.TEXTURE_UV)
            .add("UV1", NeoVertexFormat.Element.OVERLAY_UV)
            .add("Color", NeoVertexFormat.Element.COLOR)
            .add("Normal", NeoVertexFormat.Element.NORMAL)
            .build()
        @JvmField
        val BLIT_VERTEX_FORMAT = NeoVertexFormat.builder()
            .add("Position", NeoVertexFormat.Element.POSITION)
            .add("UV0", NeoVertexFormat.Element.TEXTURE_UV)
            .build()

        @JvmStatic
        fun drawState(sampler0: GlTexture2D, shader: NeoIdentifier) = GlDrawState.Basic(
            blend = GlBlendShard.Enabled(
                BlendFunction.Basic(
                    GlBlendingFactor.ONE,
                    GlBlendingFactor.ONE
                ),
                if (VibrancyConfig.limitLightBrightness) GlBlendEquation.MAX else GlBlendEquation.ADD
            ),
            cull = GlCullShard.Enabled(
                GlCullFace.BACK
            ),
            depth = GlDepthShard.Enabled(
                GlAlphaFunction.LEQUAL
            ),
            polygonOffset = GlPolygonOffsetShard.Enabled(
                PolygonOffset(
                    -1f,
                    -4f
                )
            ),
            shader = GlShaderShard.FromLocation(
                shader,
                {
                    setUniform("SpecularReflectionsEnabled") { set(if (VibrancyConfig.reflectionsEnabled) 1 else 0) }
                    setUniform("SpecularReflectionStrength") { set(VibrancyConfig.reflectionStrength) }
                    setUniform("SpecularReflectionExponent") { set(VibrancyConfig.reflectionExponent) }
                },
                GlTextureBinding.FromInstance(
                    sampler0,
                    GlTextureTarget.TEXTURE_2D
                )
            )
        )

        @JvmStatic
        fun initBlitMesh(mesh: Mesh, info: MeshData) {
            val vertexBuffer = NeoBuffer.GCNative(info.faces.size.toLong() * 4 * BLIT_VERTEX_FORMAT.vertexSizeBytes)
            val indexCount = info.faces.size * 6
            val indexType = when (indexCount) {
                indexCount and BYTE_MASK -> GlIndexDataType.BYTE
                indexCount and SHORT_MASK -> GlIndexDataType.SHORT
                else -> GlIndexDataType.INT
            }
            val indexBuffer = NeoBuffer.GCNative(indexCount.toLong() * VERTEX_FORMAT.vertexSizeBytes)

            vertexBuffer.write().run {
                fun vertex(pos: IVec3<Float>, texX: Float, texY: Float) {
                    writeFloat(pos.x)
                    writeFloat(pos.y)
                    writeFloat(pos.z)
                    writeFloat(texX / info.sections.size.x.toFloat())
                    writeFloat(texY / info.sections.size.y.toFloat())
                }

                info.faces.forEachIndexed { index, face ->
                    val texture = info.sections.textures[index]

                    vertex(face.quad.v0.pos, texture.min.x.toFloat(), texture.min.y.toFloat())
                    vertex(face.quad.v1.pos, texture.max.x.toFloat(), texture.min.y.toFloat())
                    vertex(face.quad.v2.pos, texture.max.x.toFloat(), texture.max.y.toFloat())
                    vertex(face.quad.v3.pos, texture.min.x.toFloat(), texture.max.y.toFloat())
                }
            }
            indexBuffer.write().run {
                var vertex = 0

                repeat(info.faces.size) {
                    indexType.write(this, vertex)
                    indexType.write(this, vertex + 1)
                    indexType.write(this, vertex + 2)
                    indexType.write(this, vertex + 2)
                    indexType.write(this, vertex + 3)
                    indexType.write(this, vertex)
                    vertex += 4
                }
            }

            mesh.rawUpload(indexCount, indexType, vertexBuffer, indexBuffer)
            vertexBuffer.free()
            indexBuffer.free()
        }
    }

    @JvmField
    val mesh = Mesh(
        VERTEX_FORMAT,
        GlBeginMode.QUADS,
        GlBufferWriter.Mode.REGULAR,
        usage
    )
    var empty = true
        protected set

    fun draw() {
        if (!empty) {
            mesh.draw()
        }
    }

    fun lazyUpload(
        lightFaces: List<LightFace>
    ): () -> TextureAtlas.Result {
        val textures = Array(lightFaces.size) {
            val face = lightFaces[it]
            NeoVec2i(face.width, face.height)
        }
        val result = TextureAtlas.pack(*textures)
        val vertexBuffer = NeoBuffer.GCNative(lightFaces.size.toLong() * 4 * VERTEX_FORMAT.vertexSizeBytes)

        vertexBuffer.write().run {
            lightFaces.forEachIndexed { index, face ->
                for (vertex in face.applyOverlay(result.textures[index]).vertices) {
                    writeFloat(vertex.pos.x)
                    writeFloat(vertex.pos.y)
                    writeFloat(vertex.pos.z)
                    writeFloat(vertex.textureUV!!.x)
                    writeFloat(vertex.textureUV!!.y)
                    writeShort(vertex.overlayUV!!.x)
                    writeShort(vertex.overlayUV!!.y)
                    writeInt(vertex.color!!.toRGBA())
                    writeByte((vertex.normal!!.x * 127).toInt())
                    writeByte((vertex.normal!!.y * 127).toInt())
                    writeByte((vertex.normal!!.z * 127).toInt())
                }
            }
        }

        val indices = mesh.generateIndices(lightFaces.size * 4)

        return {
            empty = lightFaces.isEmpty()

            mesh.rawUpload(lightFaces.size * 6, indices.second, vertexBuffer, indices.first)
            vertexBuffer.free()
            indices.first.free()

            result
        }
    }

    fun upload(faces: List<LightFace>): TextureAtlas.Result {
        return lazyUpload(faces)()
    }

    override fun free() {
        mesh.free()
    }

    data class MeshData(
        @JvmField
        val faces: List<LightFace>,
        @JvmField
        val sections: TextureAtlas.Result
    )
}