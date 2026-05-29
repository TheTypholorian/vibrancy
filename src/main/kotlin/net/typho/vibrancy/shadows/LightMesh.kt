package net.typho.vibrancy.shadows

import net.typho.big_shot_lib.api.client.rendering.opengl.constant.*
import net.typho.big_shot_lib.api.client.rendering.opengl.resource.bound.GlBoundProgram
import net.typho.big_shot_lib.api.client.rendering.opengl.resource.bound.GlBufferWriter
import net.typho.big_shot_lib.api.client.rendering.opengl.resource.type.GlTexture2D
import net.typho.big_shot_lib.api.client.rendering.opengl.state.*
import net.typho.big_shot_lib.api.client.rendering.opengl.util.PolygonOffset
import net.typho.big_shot_lib.api.client.rendering.util.Mesh
import net.typho.big_shot_lib.api.client.rendering.util.NeoVertexFormat
import net.typho.big_shot_lib.api.client.rendering.util.quad.NeoBakedQuad
import net.typho.big_shot_lib.api.math.vec.NeoVec2i
import net.typho.big_shot_lib.api.math.vec.NeoVec3f
import net.typho.big_shot_lib.api.util.NeoColor
import net.typho.big_shot_lib.api.util.buffer.NeoBuffer
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
        val COMPACT_POSITION = NeoVertexFormat.Element.create(0, GlDataType.UNSIGNED_BYTE, true, 3)
        @JvmField
        val COMPACT_TEXTURE_UV = NeoVertexFormat.Element.create(0, GlDataType.UNSIGNED_SHORT, true, 2)
        @JvmField
        val LIGHT_INDEX = NeoVertexFormat.Element.create(0, GlDataType.UNSIGNED_BYTE, null, 1)

        @JvmField
        val VERTEX_FORMAT = NeoVertexFormat.builder()
            .add("Position", NeoVertexFormat.Element.POSITION)
            .add("UV0", NeoVertexFormat.Element.TEXTURE_UV)
            .add("UV1", NeoVertexFormat.Element.OVERLAY_UV)
            .add("Color", NeoVertexFormat.Element.COLOR)
            .add("Normal", NeoVertexFormat.Element.NORMAL)
            .padding(1)
            .build()
        @JvmField
        val INVENTORY_VERTEX_FORMAT = NeoVertexFormat.builder()
            .add("Position", NeoVertexFormat.Element.POSITION)
            .add("UV0", NeoVertexFormat.Element.TEXTURE_UV)
            //.add("Color", NeoVertexFormat.Element.COLOR)
            //.add("Normal", NeoVertexFormat.Element.NORMAL)
            .build()
        @JvmField
        val BLIT_VERTEX_FORMAT = NeoVertexFormat.builder()
            .add("Position", NeoVertexFormat.Element.POSITION)
            .add("UV0", NeoVertexFormat.Element.TEXTURE_UV)
            .build()
        @JvmField
        val SKY_VERTEX_FORMAT = NeoVertexFormat.builder()
            .add("Position", NeoVertexFormat.Element.POSITION)
            .add("UV0", NeoVertexFormat.Element.TEXTURE_UV)
            .add("UV2", NeoVertexFormat.Element.LIGHT_UV)
            .add("Color", NeoVertexFormat.Element.COLOR)
            .add("Normal", NeoVertexFormat.Element.NORMAL)
            .padding(1)
            .build()

        @JvmStatic
        fun drawState(sampler0: GlTexture2D, shader: NeoIdentifier, uniforms: GlBoundProgram.() -> Unit = {
            setUniform("SpecularReflectionsEnabled") { set(if (VibrancyConfig.reflectionsEnabled) 1 else 0) }
            setUniform("SpecularReflectionStrength") { set(VibrancyConfig.reflectionStrength) }
            setUniform("SpecularReflectionExponent") { set(VibrancyConfig.reflectionExponent) }
        }, blend: GlBlendShard = GlBlendShard.Disabled) = GlDrawState.Basic(
            blend = blend,
            cull = GlCullShard.Enabled(
                GlCullFace.BACK
            ),
            depth = GlDepthShard.Enabled(
                GlAlphaFunction.LEQUAL,
                false
            ),
            polygonOffset = GlPolygonOffsetShard.Enabled(
                PolygonOffset(
                    -1f,
                    -4f
                )
            ),
            shader = GlShaderShard.FromLocation(
                shader,
                uniforms,
                GlTextureBinding.FromInstance(
                    sampler0,
                    GlTextureTarget.TEXTURE_2D
                )
            )
        )

        @JvmStatic
        fun initBlitMesh(mesh: Mesh, info: MeshData) {
            val vertexBuffer = NeoBuffer.GCNative(info.faces.size.toLong() * 4 * BLIT_VERTEX_FORMAT.vertexSizeBytes)

            vertexBuffer.write().run {
                fun vertex(vertex: PrimitiveVertex, texX: Float, texY: Float) {
                    writeFloat(vertex.x)
                    writeFloat(vertex.y)
                    writeFloat(vertex.z)
                    writeFloat(texX / info.sections.size.x.toFloat())
                    writeFloat(texY / info.sections.size.y.toFloat())
                }

                info.faces.forEachIndexed { index, face ->
                    val texture = info.sections.textures[index]

                    vertex(face.v0, texture.min.x.toFloat(), texture.min.y.toFloat())
                    vertex(face.v1, texture.max.x.toFloat(), texture.min.y.toFloat())
                    vertex(face.v2, texture.max.x.toFloat(), texture.max.y.toFloat())
                    vertex(face.v3, texture.min.x.toFloat(), texture.max.y.toFloat())
                }
            }

            val indices = mesh.generateIndices(info.faces.size * 4)

            mesh.rawUpload(info.faces.size * 6, indices.second, vertexBuffer, indices.first)
            vertexBuffer.free()
            indices.first.free()
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
    ): Pair<AutoCloseable, () -> TextureAtlas.Result> {
        val textures = Array(lightFaces.size) {
            val face = lightFaces[it]
            NeoVec2i(face.width, face.height)
        }
        val result = TextureAtlas.pack(*textures)
        val vertexBuffer = NeoBuffer.GCNative(lightFaces.size.toLong() * 4 * VERTEX_FORMAT.vertexSizeBytes)

        vertexBuffer.write().run {
            lightFaces.forEachIndexed { index, face ->
                val texture = result.textures[index]

                face.apply { vertex, index ->
                    writeFloat(vertex.x)
                    writeFloat(vertex.y)
                    writeFloat(vertex.z)

                    writeFloat(vertex.u)
                    writeFloat(vertex.v)

                    writeShort(if (index == 0 || index == 3) texture.min.x else texture.max.x)
                    writeShort(if (index == 0 || index == 1) texture.min.y else texture.max.y)

                    writeInt(vertex.color)
                    writeInt(vertex.normal)
                }
            }
        }

        val indices = mesh.generateIndices(lightFaces.size * 4)

        return AutoCloseable {
            vertexBuffer.free()
            indices.first.free()
        } to {
            empty = lightFaces.isEmpty()

            mesh.rawUpload(lightFaces.size * 6, indices.second, vertexBuffer, indices.first)

            result
        }
    }

    fun lazyUploadNoAtlas(
        faces: List<LightFace>
    ): () -> Unit {
        val vertexBuffer = NeoBuffer.GCNative(faces.size.toLong() * 4 * VERTEX_FORMAT.vertexSizeBytes)

        vertexBuffer.write().run {
            faces.forEachIndexed { index, face ->
                face.apply { vertex ->
                    writeFloat(vertex.x)
                    writeFloat(vertex.y)
                    writeFloat(vertex.z)

                    writeFloat(vertex.u)
                    writeFloat(vertex.v)

                    writeInt(0) // padding

                    writeInt(vertex.color)
                    writeInt(vertex.normal)
                }
            }
        }

        val indices = mesh.generateIndices(faces.size * 4)

        return {
            empty = faces.isEmpty()

            mesh.rawUpload(faces.size * 6, indices.second, vertexBuffer, indices.first)
            vertexBuffer.free()
            indices.first.free()
        }
    }

    fun lazyUploadQuadsNoAtlas(
        faces: List<NeoBakedQuad>
    ): () -> Unit {
        val vertexBuffer = NeoBuffer.GCNative(faces.size.toLong() * 4 * VERTEX_FORMAT.vertexSizeBytes)

        vertexBuffer.write().run {
            faces.forEachIndexed { index, face ->
                for (vertex in face.vertices) {
                    writeFloat(vertex.pos.x)
                    writeFloat(vertex.pos.y)
                    writeFloat(vertex.pos.z)
                    writeFloat(vertex.textureUV!!.x)
                    writeFloat(vertex.textureUV!!.y)
                    writeShort(vertex.overlayUV?.x ?: 0)
                    writeShort(vertex.overlayUV?.y ?: 0)
                    writeInt((vertex.color ?: NeoColor.FULL_ON).toRGBA())
                    val normal = vertex.normal ?: face.direction?.toFloat() ?: NeoVec3f(0f, 1f, 0f)
                    writeByte((normal.x * 127).toInt())
                    writeByte((normal.y * 127).toInt())
                    writeByte((normal.z * 127).toInt())
                    writeByte(0)
                }
            }
        }

        val indices = mesh.generateIndices(faces.size * 4)

        return {
            empty = faces.isEmpty()

            mesh.rawUpload(faces.size * 6, indices.second, vertexBuffer, indices.first)
            vertexBuffer.free()
            indices.first.free()
        }
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