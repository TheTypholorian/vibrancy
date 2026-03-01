package net.typho.vibrancy.shadows

import com.mojang.blaze3d.vertex.VertexSorting
import net.typho.big_shot_lib.api.client.opengl.buffers.*
import net.typho.big_shot_lib.api.client.opengl.state.*
import net.typho.big_shot_lib.api.client.opengl.util.*
import net.typho.big_shot_lib.api.client.util.events.RenderEventData
import net.typho.big_shot_lib.api.util.IColor
import net.typho.vibrancy.Vibrancy
import net.typho.vibrancy.block.impl.RayPointLight
import org.lwjgl.system.NativeResource

open class ShadowTexture(
    @JvmField
    val scale: Int
) : NativeResource {
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
                    { light.shadows.mesh.vbo.cast(BufferType.SHADER_STORAGE_BUFFER) },
                    0
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

                    shader.getUniform("ShadowWidth")?.setValue(light.shadows.size)
                    shader.getUniform("ShadowScale")?.setValue(light.shadows.scale)
                    shader.getUniform("LightPos")?.setValue(light.absolutePos)
                    shader.getUniform("LightColor")?.setValue(light.color)
                    shader.getUniform("LightRadius")?.setValue(light.radius)
                }
            )
        )
    }

    var size = 0
        private set
    val mesh by lazy {
        Mesh(
            VERTEX_FORMAT,
            GlShapeType.QUADS,
            BufferUsage.STATIC_DRAW
        )
    }
    val texture by lazy {
        NeoTexture2D(
            OpenGL.INSTANCE.createTexture(),
            TextureFormat.RGB16F,
            true,
            TextureType.TEXTURE_2D
        )
    }
    val target by lazy {
        NeoFramebuffer(
            listOf(texture),
            null,
            1,
            1
        )
    }

    override fun free() {
        mesh.free()
        target.free()
        texture.free()
    }

    inner class Builder {
        @JvmField
        val meshBuilder = mesh.Builder()

        fun finish(sorting: VertexSorting? = null) {
            val built = meshBuilder.buildOrThrow()
            sorting?.let { built.sortQuads(meshBuilder.buffer, it) }
            mesh.upload(built)
            meshBuilder.buffer.close()

            size = built.drawState().vertexCount / 4

            target.resize((scale * size).coerceAtLeast(1), scale.coerceAtLeast(1))
        }
    }
}