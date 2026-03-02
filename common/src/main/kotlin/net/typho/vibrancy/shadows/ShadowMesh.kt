package net.typho.vibrancy.shadows

import net.typho.big_shot_lib.api.client.opengl.buffers.*
import net.typho.big_shot_lib.api.client.opengl.state.*
import net.typho.big_shot_lib.api.client.opengl.util.GlShapeType
import net.typho.big_shot_lib.api.client.opengl.util.TextureUtil
import net.typho.big_shot_lib.api.client.util.events.RenderEventData
import net.typho.big_shot_lib.api.util.IColor
import net.typho.vibrancy.Vibrancy
import net.typho.vibrancy.block.impl.RayPointLight
import org.lwjgl.system.NativeResource

open class ShadowMesh : NativeResource {
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
                    { light.shadows.lightMesh.mesh.vbo.cast(BufferType.SHADER_STORAGE_BUFFER) },
                    1
                ),
                BindBufferBaseShard(
                    { light.shadows.lightMesh.atlas },
                    2
                ),
                FramebufferShard(
                    { light.shadows.lightMesh.target },
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

    val shadowMesh = Mesh(
        VERTEX_FORMAT,
        GlShapeType.QUADS,
        BufferUsage.STATIC_DRAW
    )
    val lightMesh = LightMesh()

    override fun free() {
        shadowMesh.free()
        lightMesh.free()
    }

    fun build(
        shadowFaces: List<LightFace>,
        lightFaces: List<LightFace>,
        atlasWidth: Int,
        atlasHeight: Int
    ): Runnable {
        val shadowBuilder = shadowMesh.Builder()

        for (face in shadowFaces) {
            face.buildGeometry(shadowBuilder)
        }

        val light = lightMesh.build(lightFaces, atlasWidth, atlasHeight)

        return Runnable {
            shadowBuilder.end()
            light.run()
        }
    }
}