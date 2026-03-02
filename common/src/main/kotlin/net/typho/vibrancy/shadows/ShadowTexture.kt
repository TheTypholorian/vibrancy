package net.typho.vibrancy.shadows

import net.typho.big_shot_lib.api.client.opengl.buffers.*
import net.typho.big_shot_lib.api.client.opengl.state.*
import net.typho.big_shot_lib.api.client.opengl.util.*
import net.typho.big_shot_lib.api.client.util.events.RenderEventData
import net.typho.big_shot_lib.api.util.IColor
import net.typho.vibrancy.TextureAtlas
import net.typho.vibrancy.Vibrancy
import net.typho.vibrancy.block.impl.RayPointLight
import org.joml.Vector2f
import org.joml.Vector3f
import org.lwjgl.system.NativeResource
import java.awt.Dimension
import java.awt.Rectangle
import java.util.*
import kotlin.math.abs

open class ShadowTexture : NativeResource {
    companion object {
        @JvmField
        val SHADOW_VERTEX_FORMAT = NeoVertexFormat.builder()
            .add("Position", NeoVertexFormat.Element.POSITION)
            .padding(Float.SIZE_BYTES)
            .add("UV0", NeoVertexFormat.Element.TEXTURE_UV)
            .padding(2 * Float.SIZE_BYTES)
            .build()
        @JvmField
        val LIGHT_VERTEX_FORMAT = NeoVertexFormat.builder()
            .add("Position", NeoVertexFormat.Element.POSITION)
            .add("UV0", NeoVertexFormat.Element.TEXTURE_UV)
            .add("UV1", NeoVertexFormat.Element.OVERLAY_UV)
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
                    { light.shadows.atlas },
                    1
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
    val scale: Int
        get() = Vibrancy.config.shadowResolution
    val atlas = GlBuffer(
        BufferType.SHADER_STORAGE_BUFFER,
        BufferUsage.STATIC_DRAW
    )
    val shadowMesh = Mesh(
        SHADOW_VERTEX_FORMAT,
        GlShapeType.QUADS,
        BufferUsage.STATIC_DRAW
    )
    val lightMesh = Mesh(
        LIGHT_VERTEX_FORMAT,
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

    private data class MeshFace(
        val v1: Vector3f,
        val v2: Vector3f,
        val v3: Vector3f,
        val v4: Vector3f,

        val uv1: Vector2f,
        val uv2: Vector2f,
        val uv3: Vector2f,
        val uv4: Vector2f,

        val texture: Rectangle
    ) {
        fun buildGeometry(builder: NeoVertexConsumer) {
            builder.vertex(v1)
                .textureUV(uv1)
                .overlayUV(texture.x, texture.y)
            builder.vertex(v2)
                .textureUV(uv2)
                .overlayUV(texture.x, texture.y + texture.height)
            builder.vertex(v3)
                .textureUV(uv3)
                .overlayUV(texture.x + texture.width, texture.y + texture.height)
            builder.vertex(v4)
                .textureUV(uv4)
                .overlayUV(texture.x + texture.width, texture.y)
        }
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

        init {
            for (face in shadowFaces) {
                face.buildGeometry(shadowBuilder)
            }

            for (face in lightFaces) {
                textures.add(Dimension(
                    (abs(face.quad.uv2.x - face.quad.uv1.x) * atlasWidth * face.width).toInt(),
                    (abs(face.quad.uv4.y - face.quad.uv1.y) * atlasHeight * face.height).toInt()
                ))
            }
        }

        fun finish() {
            val result = TextureAtlas.pack(*textures.toTypedArray())
            this.result = result

            lightFaces.forEachIndexed { index, face ->
                val texture = result.textures[index]

                fun accept(
                    v1: Vector3f,
                    v2: Vector3f,
                    v3: Vector3f,
                    v4: Vector3f,
                    texture: Rectangle
                ) {
                    MeshFace(
                        v1,
                        v2,
                        v3,
                        v4,

                        face.quad.uv1,
                        face.quad.uv2,
                        face.quad.uv3,
                        face.quad.uv4,

                        texture
                    ).buildGeometry(lightBuilder)
                }

                if (face.width == 1 && face.height == 1) {
                    accept(face.quad.v1, face.quad.v2, face.quad.v3, face.quad.v4, texture)
                } else {
                    val xInc = 1f / face.width
                    val yInc = 1f / face.height

                    repeat(face.width) { x ->
                        repeat(face.height) { y ->
                            val fx = x.toFloat() / face.width
                            val fy = y.toFloat() / face.height

                            val a1 = face.quad.v1.lerp(face.quad.v2, fx, Vector3f())
                            val b1 = face.quad.v4.lerp(face.quad.v3, fx, Vector3f())

                            val a2 = face.quad.v1.lerp(face.quad.v2, fx + xInc, Vector3f())
                            val b2 = face.quad.v4.lerp(face.quad.v3, fx + xInc, Vector3f())

                            accept(
                                a1.lerp(b1, fy, Vector3f()),
                                a1.lerp(b1, fy + yInc, Vector3f()),
                                a2.lerp(b2, fy + yInc, Vector3f()),
                                a2.lerp(b2, fy, Vector3f()),
                                Rectangle(
                                    (texture.x + texture.width * fx).toInt(),
                                    (texture.y + texture.height * fy).toInt(),
                                    (texture.width * xInc).toInt(),
                                    (texture.height * yInc).toInt(),
                                )
                            )
                        }
                    }
                }
            }
        }

        fun upload() {
            val built = shadowBuilder.buildOrThrow()
            shadowMesh.upload(built)
            shadowBuilder.buffer.close()

            size = built.drawState().vertexCount / 4

            lightBuilder.end()

            texture.resize(result!!.width, result!!.height)
            TextureAtlas.store(result!!, atlas)
        }
    }
}