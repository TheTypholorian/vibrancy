package net.typho.vibrancy.shadows

import com.mojang.blaze3d.vertex.ByteBufferBuilder
import net.minecraft.world.level.Level
import net.typho.big_shot_lib.api.client.opengl.buffers.*
import net.typho.big_shot_lib.api.client.opengl.shaders.GlShader
import net.typho.big_shot_lib.api.client.opengl.state.*
import net.typho.big_shot_lib.api.client.opengl.util.FogUtil
import net.typho.big_shot_lib.api.client.opengl.util.GlResourcePool
import net.typho.big_shot_lib.api.client.opengl.util.GlShapeType
import net.typho.big_shot_lib.api.client.opengl.util.TextureFormat
import net.typho.big_shot_lib.api.client.util.events.RenderEventData
import net.typho.big_shot_lib.api.util.IColor
import net.typho.vibrancy.TextureAtlas
import net.typho.vibrancy.Vibrancy
import org.lwjgl.system.NativeResource
import java.awt.Dimension

open class LightMesh : NativeResource {
    companion object {
        @JvmField
        val VERTEX_FORMAT = NeoVertexFormat.builder()
            .add("Position", NeoVertexFormat.Element.POSITION)
            .add("UV0", NeoVertexFormat.Element.TEXTURE_UV)
            .add("UV1", NeoVertexFormat.Element.OVERLAY_UV)
            .add("Color", NeoVertexFormat.Element.COLOR)
            .build()
        @JvmField
        val BLIT_VERTEX_FORMAT = NeoVertexFormat.builder()
            .add("Position", NeoVertexFormat.Element.POSITION)
            .add("UV0", NeoVertexFormat.Element.TEXTURE_UV)
            .build()
        @JvmField
        val pool = GlResourcePool(
            { LightMesh() },
            512,
            { LightMesh() },
            true
        )

        @JvmStatic
        fun renderSettings(fbo: GlFramebuffer, data: RenderEventData, sampler0: GlTexture) =
            RenderSettings(
                Vibrancy.id("light_mesh"),
                listOf(
                    FramebufferShard(
                        { fbo },
                        true
                    ),
                    BlendShard(
                        true,
                        IColor.FULL_ON,
                        BlendEquation.ADD,
                        BlendFunction.Basic(
                            BlendFactor.SRC_ALPHA,
                            BlendFactor.ONE
                        )
                    ),
                    CullShard(
                        true,
                        CullFace.BACK
                    ),
                    DepthMaskShard(
                        false
                    ),
                    DepthTestShard(
                        true,
                        ComparisonFunc.LEQUAL
                    ),
                    PolygonOffsetShard(
                        PolygonOffset(
                            -1f,
                            -4f,
                        )
                    ),
                    ShaderShard(
                        Vibrancy.id("light_mesh")
                    ) { shader ->
                        shader.setCommonUniforms(data)

                        shader.getUniform("Sampler0")?.setSampler(sampler0)
                        FogUtil.INSTANCE.upload(shader)
                    }
                )
            )

        private val lightBlitMesh by lazy {
            Mesh(
                BLIT_VERTEX_FORMAT,
                GlShapeType.QUADS,
                BufferUsage.STREAM_DRAW
            )
        }

        @JvmStatic
        fun initBlitMesh(mesh: Mesh, info: LightBlitInfo) {
            val builder = mesh.Builder(ByteBufferBuilder(info.lightFaces.size * 4 * BLIT_VERTEX_FORMAT.vertexSizeBytes))

            info.lightFaces.forEachIndexed { index, face ->
                val texture = info.atlasResult.textures[index]
                builder.vertex(face.quad.v0.pos)
                    .textureUV(
                        texture.x.toFloat() / info.atlasResult.width,
                        texture.y.toFloat() / info.atlasResult.height
                    )
                builder.vertex(face.quad.v1.pos)
                    .textureUV(
                        (texture.x.toFloat() + texture.width) / info.atlasResult.width,
                        texture.y.toFloat() / info.atlasResult.height
                    )
                builder.vertex(face.quad.v2.pos)
                    .textureUV(
                        (texture.x.toFloat() + texture.width) / info.atlasResult.width,
                        (texture.y.toFloat() + texture.height) / info.atlasResult.height
                    )
                builder.vertex(face.quad.v3.pos)
                    .textureUV(
                        texture.x.toFloat() / info.atlasResult.width,
                        (texture.y.toFloat() + texture.height) / info.atlasResult.height
                    )
            }

            builder.end()
        }

        @JvmStatic
        fun blitLight(info: LightBlitInfo) {
            initBlitMesh(lightBlitMesh, info)
            lightBlitMesh.draw()
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
        GlShapeType.QUADS,
        BufferUsage.STATIC_DRAW
    )
    @JvmField
    val texture = NeoTexture2D(
        TextureFormat.R11F_G11F_B10F
    )
    @JvmField
    val target = NeoFramebuffer(
        listOf(texture),
        null,
        1,
        1
    )
    var empty = true
        protected set

    fun draw(shader: GlShader) {
        if (!empty) {
            shader.getUniform("Sampler1")?.setSampler(texture)
            mesh.draw()
        }
    }

    fun build(
        level: Level?,
        lightFaces: List<LightFace>
    ): () -> TextureAtlas.Result {
        val textures = Array(lightFaces.size) {
            val face = lightFaces[it]
            Dimension(face.width, face.height)
        }
        val lightBuilder = mesh.Builder(ByteBufferBuilder(lightFaces.size * 4 * VERTEX_FORMAT.vertexSizeBytes))
        val result = TextureAtlas.pack(*textures)

        lightFaces.forEachIndexed { index, face -> face.buildGeometry(lightBuilder, result.textures[index], level) }

        return {
            empty = lightFaces.isEmpty()

            if (empty) {
                lightBuilder.buffer.close()
            } else {
                lightBuilder.end()

                target.resize(result.width.coerceAtLeast(1), result.height.coerceAtLeast(1))
            }

            result
        }
    }

    override fun free() {
        mesh.free()
        target.free()
        texture.free()
    }
}