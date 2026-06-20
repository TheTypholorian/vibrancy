package net.typho.vibrancy.shadows

import com.mojang.blaze3d.vertex.VertexFormat
import com.mojang.blaze3d.vertex.VertexFormatElement
import net.minecraft.resources.Identifier
import net.typho.big_shot_lib.api.client.rendering.opengl.constant.*
import net.typho.big_shot_lib.api.client.rendering.opengl.resource.bound.GlBoundProgram
import net.typho.big_shot_lib.api.client.rendering.opengl.resource.bound.GlBufferWriter
import net.typho.big_shot_lib.api.client.rendering.opengl.resource.GlTexture2D
import net.typho.big_shot_lib.api.client.rendering.opengl.state.*
import net.typho.big_shot_lib.api.client.rendering.opengl.util.PolygonOffset
import net.typho.big_shot_lib.api.client.rendering.util.Mesh
import net.typho.big_shot_lib.api.client.rendering.util.NeoVertexFormat
import net.typho.big_shot_lib.api.client.rendering.util.NeoVertexFormats
import net.typho.big_shot_lib.api.client.rendering.util.quad.NeoBakedQuad
import net.typho.big_shot_lib.api.math.IVec3
import net.typho.big_shot_lib.api.util.NeoColor
import net.typho.big_shot_lib.api.util.buffer.NeoBuffer
import net.typho.vibrancy.TextureAtlas
import net.typho.vibrancy.Vibrancy
import net.typho.vibrancy.VibrancyConfig
import org.lwjgl.system.NativeResource

open class LightMesh(
    @JvmField
    val usage: GlBufferUsage
) : NativeResource {
    companion object {
        @JvmField
        val COMPACT_TEXTURE_UV = NeoVertexFormats.element(0, GlDataType.USHORT, GlVertexElementReadType.INT_TO_FLOAT, 2)
        @JvmField
        val LIGHT_INDEX = NeoVertexFormats.element(0, GlDataType.USHORT, GlVertexElementReadType.INT_TO_INT, 1)

        @JvmField
        val VERTEX_FORMAT = VertexFormat.builder()
            .add("Position", VertexFormatElement.POSITION)
            .add("UV0", VertexFormatElement.TEXTURE_UV)
            .add("UV1", VertexFormatElement.OVERLAY_UV)
            .add("Color", VertexFormatElement.COLOR)
            .add("Normal", VertexFormatElement.NORMAL)
            .padding(1)
            .build(Vibrancy.id("light_mesh"))
        @JvmField
        val SKY_VERTEX_FORMAT = NeoVertexFormat.builder()
            .add("Position", VertexFormatElement.POSITION)
            .add("UV0", VertexFormatElement.TEXTURE_UV)
            .add("UV2", VertexFormatElement.LIGHT_UV)
            .add("Color", VertexFormatElement.COLOR)
            .add("Normal", VertexFormatElement.NORMAL)
            .padding(1)
            .build(Vibrancy.id("sky_mesh"))

        @JvmStatic
        fun drawState(sampler0: GlTexture2D, shader: Identifier, uniforms: GlBoundProgram.() -> Unit = {
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
                    -10f
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
        lightFaces: List<BlockFace>
    ): Pair<AutoCloseable, () -> TextureAtlas.Result> {
        val textures = Array(lightFaces.size) {
            val face = lightFaces[it]
            IVec2(face.width, face.height)
        }
        val result = TextureAtlas.pack(textures.asList())
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

    fun lazyUpload(
        lightFaces: List<Pair<IVec3<Int>, List<BlockFace>>>,
        numFaces: Int
    ): Pair<AutoCloseable, () -> TextureAtlas.Result> {
        val textures = lightFaces.flatMap { entry ->
            entry.second.map { IVec2(it.width, it.height) }
        }
        val result = TextureAtlas.pack(textures)
        val vertexBuffer = NeoBuffer.GCNative(numFaces.toLong() * 4 * VERTEX_FORMAT.vertexSizeBytes)
        var textureIndex = 0

        vertexBuffer.write().run {
            lightFaces.forEach { entry ->
                entry.second.forEach { face ->
                    val texture = result.textures[textureIndex++]

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
        }

        val indices = mesh.generateIndices(numFaces * 4)

        return AutoCloseable {
            vertexBuffer.free()
            indices.first.free()
        } to {
            empty = numFaces == 0

            mesh.rawUpload(numFaces * 6, indices.second, vertexBuffer, indices.first)

            result
        }
    }

    fun lazyUploadNoAtlas(
        faces: List<BlockFace>
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
                    val normal = vertex.normal ?: face.direction?.toFloat() ?: IVec3(0f, 1f, 0f)
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

    data class FlatMeshData(
        @JvmField
        val faces: List<BlockFace>,
        @JvmField
        val sections: TextureAtlas.Result
    )

    data class ComplexMeshData(
        @JvmField
        val faces: List<Pair<IVec3<Int>, List<BlockFace>>>,
        @JvmField
        val numFaces: Int,
        @JvmField
        val sections: TextureAtlas.Result
    )
}