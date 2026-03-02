package net.typho.vibrancy.shadows

import net.typho.big_shot_lib.api.client.opengl.buffers.*
import net.typho.big_shot_lib.api.client.opengl.state.*
import net.typho.big_shot_lib.api.client.opengl.util.*
import net.typho.big_shot_lib.api.client.util.events.RenderEventData
import net.typho.big_shot_lib.api.util.IColor
import net.typho.vibrancy.TextureAtlas
import net.typho.vibrancy.Vibrancy
import net.typho.vibrancy.block.impl.RayPointLight
import org.lwjgl.system.NativeResource
import java.awt.Dimension
import java.util.*
import kotlin.math.abs

open class ShadowTexture : NativeResource {
    companion object {
        @JvmField
        val VERTEX_FORMAT = NeoVertexFormat.builder()
            .add("Position", NeoVertexFormat.Element.POSITION)
            .padding(Float.SIZE_BYTES)
            .add("UV0", NeoVertexFormat.Element.TEXTURE_UV)
            .padding(2 * Float.SIZE_BYTES)
            .build()

        @JvmStatic
        fun blitSettings(data: RenderEventData, light: RayPointLight) = RenderSettings(
            Vibrancy.id("block/raytraced/blit"),
            listOf(
                DisableFlagsShard(listOf(
                    GlFlag.DEPTH_TEST,
                    GlFlag.CULL_FACE,
                    GlFlag.BLEND
                )),
                BindBufferBaseShard(
                    { light.shadows.shadowMesh.vbo.cast(BufferType.SHADER_STORAGE_BUFFER) },
                    0
                ),
                BindBufferBaseShard(
                    { light.shadows.lightMesh.vbo.cast(BufferType.SHADER_STORAGE_BUFFER) },
                    1
                ),
                BindBufferBaseShard(
                    { light.shadows.atlas },
                    2
                ),
                FramebufferShard(
                    { light.shadows.target },
                    true,
                    ClearBit.Color(IColor.FULL_OFF)
                ),
                ShaderShard(
                    Vibrancy.id("block/raytraced/blit")
                ) { shader ->
                    shader.setCommonUniforms(data)
                    shader.getUniform("Sampler0")?.setSampler(TextureUtil.INSTANCE.getMinecraftTexture(TextureUtil.INSTANCE.blockAtlasTexture))

                    shader.getUniform("LightPos")?.setValue(light.absolutePos)
                    shader.getUniform("LightColor")?.setValue(light.color)
                    shader.getUniform("LightRadius")?.setValue(light.radius)
                }
            )
        )
    }

    val atlas = GlBuffer(
        BufferType.SHADER_STORAGE_BUFFER,
        BufferUsage.STATIC_DRAW
    )
    val shadowMesh = Mesh(
        VERTEX_FORMAT,
        GlShapeType.QUADS,
        BufferUsage.STATIC_DRAW
    )
    val lightMesh = Mesh(
        VERTEX_FORMAT,
        GlShapeType.QUADS,
        BufferUsage.STATIC_DRAW
    )
    val texture = NeoTexture2D(
        OpenGL.INSTANCE.createTexture(),
        TextureFormat.RGB16F,
        true,
        TextureType.TEXTURE_2D
    )
    val target = NeoFramebuffer(
        listOf(texture),
        null,
        1,
        1
    )

    override fun free() {
        atlas.free()
        shadowMesh.free()
        lightMesh.free()
        target.free()
        texture.free()
    }

    inner class Builder(
        private val shadowFaces: List<LightFace>,
        private val lightFaces: List<LightFace>,
        private val atlasWidth: Int,
        private val atlasHeight: Int
    ) {
        private val textures = LinkedList<Dimension>()
        private var result: TextureAtlas.Result? = null
        private val shadowBuilder = shadowMesh.Builder()
        private val lightBuilder = lightMesh.Builder()

        fun finish() {
            for (face in shadowFaces) {
                face.buildGeometry(shadowBuilder)
            }

            for (face in lightFaces) {
                face.buildGeometry(lightBuilder)
                textures.add(Dimension(
                    (abs(face.quad.uv1.x - face.quad.uv3.x) * atlasWidth * face.width).toInt(),
                    (abs(face.quad.uv1.y - face.quad.uv3.y) * atlasHeight * face.height).toInt()
                ))
            }

            result = TextureAtlas.pack(*textures.toTypedArray())
        }

        fun upload() {
            shadowBuilder.end()
            lightBuilder.end()

            target.resize(result!!.width, result!!.height)
            TextureAtlas.store(result!!, atlas)
        }
    }
}