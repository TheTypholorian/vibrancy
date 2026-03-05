package net.typho.vibrancy.shadows

import com.mojang.blaze3d.vertex.ByteBufferBuilder
import net.minecraft.world.level.Level
import net.typho.big_shot_lib.api.client.opengl.buffers.*
import net.typho.big_shot_lib.api.client.opengl.state.*
import net.typho.big_shot_lib.api.client.opengl.util.FogUtil
import net.typho.big_shot_lib.api.client.opengl.util.GlShapeType
import net.typho.big_shot_lib.api.client.opengl.util.TextureFormat
import net.typho.big_shot_lib.api.client.util.events.RenderEventData
import net.typho.big_shot_lib.api.util.IColor
import net.typho.vibrancy.TextureAtlas
import net.typho.vibrancy.Vibrancy
import org.lwjgl.system.NativeResource
import java.awt.Dimension
import java.util.*

open class LightMesh : NativeResource {
    companion object {
        @JvmField
        val VERTEX_FORMAT = NeoVertexFormat.builder()
            .add("Position", NeoVertexFormat.Element.POSITION)
            .padding(4)
            .add("UV0", NeoVertexFormat.Element.TEXTURE_UV)
            .add("Color", NeoVertexFormat.Element.COLOR)
            .padding(4)
            .build()

        @JvmStatic
        fun renderSettings(fbo: GlFramebuffer, mesh: LightMesh, data: RenderEventData, sampler0: GlTexture) =
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
                            -1f,
                        )
                    ),
                    BindBufferBaseShard(
                        { mesh.atlas },
                        0
                    ),
                    ShaderShard(
                        Vibrancy.id("light_mesh")
                    ) { shader ->
                        shader.setCommonUniforms(data)

                        shader.getUniform("Sampler0")?.setSampler(sampler0)
                        shader.getUniform("Sampler1")?.setSampler(mesh.texture)
                        FogUtil.INSTANCE.upload(shader)
                    }
                )
            )
    }

    @JvmField
    val atlas = GlBuffer(
        BufferType.SHADER_STORAGE_BUFFER,
        BufferUsage.STATIC_DRAW
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

    fun draw(fbo: GlFramebuffer, data: RenderEventData, sampler0: GlTexture) {
        if (!empty) {
            val settings = renderSettings(fbo, this, data, sampler0)

            settings.bind()
            mesh.draw()
            settings.unbind()
        }
    }

    fun build(
        level: Level?,
        lightFaces: List<LightFace>
    ): Runnable {
        val textures = LinkedList<Dimension>()
        val lightBuilder = mesh.Builder(ByteBufferBuilder(lightFaces.size * 4 * VERTEX_FORMAT.vertexSizeBytes))
        var empty = true

        for (face in lightFaces) {
            face.buildGeometry(lightBuilder, level)
            textures.add(Dimension(
                face.quad.sprite.height,
                face.quad.sprite.width
            ))
            empty = false
        }

        val result = TextureAtlas.pack(*textures.toTypedArray())

        return Runnable {
            this.empty = empty

            if (empty) {
                lightBuilder.buffer.close()
            } else {
                lightBuilder.end()

                target.resize(result.width.coerceAtLeast(1), result.height.coerceAtLeast(1))
                TextureAtlas.store(result, atlas)
            }
        }
    }

    override fun free() {
        atlas.free()
        mesh.free()
        target.free()
        texture.free()
    }
}