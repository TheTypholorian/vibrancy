package net.typho.vibrancy.shadows

import net.typho.big_shot_lib.api.client.rendering.opengl.constant.*
import net.typho.big_shot_lib.api.client.rendering.opengl.resource.bound.GlBoundProgram
import net.typho.big_shot_lib.api.client.rendering.opengl.resource.bound.GlBufferWriter
import net.typho.big_shot_lib.api.client.rendering.opengl.resource.impl.NeoGlTexture2D
import net.typho.big_shot_lib.api.client.rendering.opengl.resource.type.GlTexture2D
import net.typho.big_shot_lib.api.client.rendering.opengl.state.*
import net.typho.big_shot_lib.api.client.rendering.opengl.util.BlendFunction
import net.typho.big_shot_lib.api.client.rendering.opengl.util.PolygonOffset
import net.typho.big_shot_lib.api.client.rendering.util.Mesh
import net.typho.big_shot_lib.api.client.rendering.util.NeoVertexFormat
import net.typho.big_shot_lib.api.math.vec.AbstractVec3
import net.typho.big_shot_lib.api.math.vec.NeoVec2i
import net.typho.big_shot_lib.api.util.buffer.BYTE_MASK
import net.typho.big_shot_lib.api.util.buffer.NeoBuffer
import net.typho.big_shot_lib.api.util.buffer.SHORT_MASK
import net.typho.big_shot_lib.api.util.resource.NeoIdentifier
import net.typho.vibrancy.TextureAtlas
import net.typho.vibrancy.Vibrancy
import org.lwjgl.system.NativeResource

open class LightMesh : NativeResource {
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
                if (Vibrancy.config.limitLightBrightness) GlBlendEquation.MAX else GlBlendEquation.ADD
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
                    setUniform("SpecularReflectionsEnabled") { set(if (Vibrancy.config.specularReflections.enabled) 1 else 0) }
                    setUniform("SpecularReflectionStrength") { set(Vibrancy.config.specularReflections.strength) }
                    setUniform("SpecularReflectionExponent") { set(Vibrancy.config.specularReflections.exponent) }
                },
                GlTextureBinding.FromInstance(
                    sampler0,
                    GlTextureTarget.TEXTURE_2D
                )
            )
        )

        private val lightBlitMesh by lazy {
            Mesh(
                BLIT_VERTEX_FORMAT,
                GlBeginMode.QUADS,
                GlBufferWriter.Mode.REGULAR,
                GlBufferUsage.STREAM_DRAW
            )
        }

        @JvmStatic
        fun initBlitMesh(mesh: Mesh, info: LightBlitInfo) {
            val vertexBuffer = NeoBuffer.Native(info.lightFaces.size.toLong() * 4 * BLIT_VERTEX_FORMAT.vertexSizeBytes)
            val indexCount = info.lightFaces.size * 6
            val indexType = when (indexCount) {
                indexCount and BYTE_MASK -> GlIndexDataType.BYTE
                indexCount and SHORT_MASK -> GlIndexDataType.SHORT
                else -> GlIndexDataType.INT
            }
            val indexBuffer = NeoBuffer.Native(indexCount.toLong() * VERTEX_FORMAT.vertexSizeBytes)

            vertexBuffer.write().run {
                fun vertex(pos: AbstractVec3<Float>, texX: Float, texY: Float) {
                    writeFloat(pos.x)
                    writeFloat(pos.y)
                    writeFloat(pos.z)
                    writeFloat(texX / info.atlasResult.size.x.toFloat())
                    writeFloat(texY / info.atlasResult.size.y.toFloat())
                }

                info.lightFaces.forEachIndexed { index, face ->
                    val texture = info.atlasResult.textures[index]

                    vertex(face.quad.v0.pos, texture.min.x.toFloat(), texture.min.y.toFloat())
                    vertex(face.quad.v1.pos, texture.max.x.toFloat(), texture.min.y.toFloat())
                    vertex(face.quad.v2.pos, texture.max.x.toFloat(), texture.max.y.toFloat())
                    vertex(face.quad.v3.pos, texture.min.x.toFloat(), texture.max.y.toFloat())
                }
            }
            indexBuffer.write().run {
                var vertex = 0

                repeat(info.lightFaces.size) {
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

        @JvmStatic
        fun blitLight(info: LightBlitInfo) {
            synchronized(lightBlitMesh) {
                initBlitMesh(lightBlitMesh, info)
                lightBlitMesh.draw()
            }
        }
    }

    data class LightBlitInfo(
        @JvmField
        val atlasResult: TextureAtlas.Result,
        @JvmField
        val lightFaces: List<LightFace>
    )

    @JvmField
    val mesh = Mesh(
        VERTEX_FORMAT,
        GlBeginMode.QUADS,
        GlBufferWriter.Mode.REGULAR,
        GlBufferUsage.STATIC_DRAW
    )
    @JvmField
    val texture = NeoGlTexture2D().also {
        it.bind(GlTextureTarget.TEXTURE_2D).use { texture ->
            texture.textureDataMutable(1, 1, GlTextureFormat.RGB8)
            texture.minFilter = GlTextureMinFilter.NEAREST
            texture.magFilter = GlTextureMagFilter.NEAREST
        }
    }
    var empty = true
        protected set

    fun draw(shader: GlBoundProgram) {
        if (!empty) {
            shader.setTexture(1, GlTextureBinding.FromInstance(texture, GlTextureTarget.TEXTURE_2D))
            mesh.draw()
        }
    }

    fun build(
        lightFaces: List<LightFace>
    ): () -> TextureAtlas.Result {
        val textures = Array(lightFaces.size) {
            val face = lightFaces[it]
            NeoVec2i(face.width, face.height)
        }
        val result = TextureAtlas.pack(*textures)
        val vertexBuffer = NeoBuffer.Native(lightFaces.size.toLong() * 4 * VERTEX_FORMAT.vertexSizeBytes)
        val indexCount = lightFaces.size * 6
        val indexType = when (indexCount) {
            indexCount and BYTE_MASK -> GlIndexDataType.BYTE
            indexCount and SHORT_MASK -> GlIndexDataType.SHORT
            else -> GlIndexDataType.INT
        }
        val indexBuffer = NeoBuffer.Native(indexCount.toLong() * VERTEX_FORMAT.vertexSizeBytes)

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
        indexBuffer.write().run {
            var vertex = 0

            repeat(lightFaces.size) {
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
            empty = lightFaces.isEmpty()

            mesh.rawUpload(indexCount, indexType, vertexBuffer, indexBuffer)
            vertexBuffer.free()
            indexBuffer.free()

            if (!empty) {
                val width = result.size.x.coerceAtLeast(1)
                val height = result.size.y.coerceAtLeast(1)

                if (width != texture.width || height != texture.height) {
                    texture.bind(GlTextureTarget.TEXTURE_2D).use {
                        it.textureDataMutable(width, height, GlTextureFormat.RGB8)
                    }
                }
            }

            result
        }
    }

    override fun free() {
        mesh.free()
        texture.free()
    }
}