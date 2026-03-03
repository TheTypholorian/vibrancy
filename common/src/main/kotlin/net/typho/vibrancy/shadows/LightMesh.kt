package net.typho.vibrancy.shadows

import net.typho.big_shot_lib.api.client.opengl.buffers.*
import net.typho.big_shot_lib.api.client.opengl.state.*
import net.typho.big_shot_lib.api.client.opengl.util.GlShapeType
import net.typho.big_shot_lib.api.client.opengl.util.TextureFormat
import net.typho.big_shot_lib.api.client.util.events.RenderEventData
import net.typho.big_shot_lib.api.util.IColor
import net.typho.vibrancy.TextureAtlas
import net.typho.vibrancy.Vibrancy
import org.lwjgl.opengl.GL11.glPolygonOffset
import org.lwjgl.system.NativeResource
import java.awt.Dimension
import java.util.*
import kotlin.math.abs

open class LightMesh : NativeResource {
    companion object {
        @JvmField
        val VERTEX_FORMAT = NeoVertexFormat.builder()
            .add("Position", NeoVertexFormat.Element.POSITION)
            .padding(Float.SIZE_BYTES)
            .add("UV0", NeoVertexFormat.Element.TEXTURE_UV)
            .padding(2 * Float.SIZE_BYTES)
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
                    }
                )
            )
    }

    val atlas = GlBuffer(
        BufferType.SHADER_STORAGE_BUFFER,
        BufferUsage.STATIC_DRAW
    )
    val mesh = Mesh(
        VERTEX_FORMAT,
        GlShapeType.QUADS,
        BufferUsage.STATIC_DRAW
    )
    val texture = NeoTexture2D(
        TextureFormat.RGB8
    )
    val target = NeoFramebuffer(
        listOf(texture),
        null,
        1,
        1
    )
    @JvmField
    protected var empty = true

    fun draw(fbo: GlFramebuffer, data: RenderEventData, sampler0: GlTexture) {
        if (!empty) {
            val settings = renderSettings(fbo, this, data, sampler0)

            GlFlag.POLYGON_OFFSET_FILL.stack.push(true) // TODO polygon offset shard
            glPolygonOffset(-1f, -1f)

            settings.bind()
            mesh.draw()
            settings.unbind()

            GlFlag.POLYGON_OFFSET_FILL.stack.pop()
        }
    }

    fun build(
        lightFaces: List<LightFace>,
        atlasWidth: Int,
        atlasHeight: Int
    ): Runnable {
        val textures = LinkedList<Dimension>()
        val lightBuilder = mesh.Builder()
        var empty = true

        for (face in lightFaces) {
            face.buildGeometry(lightBuilder)
            textures.add(Dimension(
                (abs(face.quad.uv1.x - face.quad.uv3.x) * atlasWidth * face.width).toInt(),
                (abs(face.quad.uv1.y - face.quad.uv3.y) * atlasHeight * face.height).toInt()
            ))
            empty = false
        }

        val result = TextureAtlas.pack(*textures.toTypedArray())

        return Runnable {
            this.empty = empty

            if (!empty) {
                lightBuilder.end()

                target.resize(result.width.coerceAtLeast(1), result.height.coerceAtLeast(1))
                TextureAtlas.store(result, atlas)
            }
        }
    }

    override fun free() {
        atlas.free()
        mesh.free()
        texture.free()
        target.free()
    }
}