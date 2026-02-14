package net.typho.vibrancy.shadows

import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.vertex.*
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.resources.ResourceLocation
import net.typho.big_shot_lib.api.client.rendering.buffers.BufferUsage
import net.typho.big_shot_lib.api.client.rendering.meshes.Mesh
import net.typho.big_shot_lib.api.client.rendering.services.TextureUtil
import net.typho.big_shot_lib.api.client.rendering.shaders.GlShader
import net.typho.big_shot_lib.api.client.rendering.state.ComparisonFunc
import net.typho.big_shot_lib.api.client.rendering.state.GlFlag
import net.typho.big_shot_lib.api.client.rendering.state.OpenGL
import net.typho.big_shot_lib.api.client.rendering.textures.*
import net.typho.big_shot_lib.api.util.resources.ResourceIdentifier
import net.typho.vibrancy.util.EmptyVertexConsumer
import org.lwjgl.system.NativeResource
import java.util.*
import java.util.function.Consumer
import kotlin.jvm.optionals.getOrNull

open class ShadowTexture(
    @JvmField
    val width: Int,
    @JvmField
    val height: Int
) : NativeResource {
    val target by lazy {
        val fbo = NeoFramebuffer(
            listOf(),
            NeoTextureCube(TextureFormat.DEPTH_COMPONENT),
            width,
            height
        )

        val texture = fbo.depthAttachment!! as GlTextureCube

        texture.bind()
        texture.setInterpolation(InterpolationType.NEAREST)
        texture.setCompareMode(TextureComparisonMode.COMPARE_REF_TO_TEXTURE)
        texture.setCompareFunc(ComparisonFunc.LEQUAL)
        texture.unbind()

        return@lazy fbo
    }
    @JvmField
    var shadows: Collection<LightFace> = emptyList()
    @JvmField
    val toFree = LinkedList<ByteBufferBuilder>()
    @JvmField
    var size = 0

    override fun free() {
        target.free()
    }

    fun getRenderTypeTexture(renderType: RenderType): ResourceLocation? {
        return when (renderType) {
            is RenderType.CompositeRenderType -> {
                renderType.state().textureState.cutoutTexture().getOrNull()
            }
            else -> null
        }
    }

    fun begin(shader: GlShader, uniforms: Consumer<GlShader>) = Builder(shader, uniforms)

    inner class Builder(
        @JvmField
        val shader: GlShader,
        @JvmField
        val uniforms: Consumer<GlShader>
    ) : MultiBufferSource {
        val builders = HashMap<ResourceIdentifier, BufferBuilder>()

        override fun getBuffer(renderType: RenderType): VertexConsumer {
            val texture = getRenderTypeTexture(renderType)

            return if (
                texture == null
                || renderType.mode() != VertexFormat.Mode.QUADS
                || !renderType.format().contains(VertexFormatElement.POSITION)
                || !renderType.format().contains(VertexFormatElement.UV0)
            ) {
                EmptyVertexConsumer
            } else {
                builders.computeIfAbsent(ResourceIdentifier(texture.namespace, texture.path)) { // TODO
                    val builder = ByteBufferBuilder(renderType.bufferSize())
                    toFree.add(builder)
                    BufferBuilder(builder, VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX)
                }
            }
        }

        fun finish() {
            target.bind()

            GlStateManager._viewport(0, 0, target.width(), target.height())
            target.clear(ClearBit.Depth(1f))

            shader.bind()
            uniforms.accept(shader)

            GlFlag.CULL_FACE.disable()
            GlFlag.DEPTH_TEST.enable()
            OpenGL.INSTANCE.depthFunc(ComparisonFunc.ALWAYS)
            GlFlag.BLEND.disable()

            size = 0

            if (!builders.isEmpty()) {
                val mesh = Mesh(DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.QUADS, BufferUsage.DYNAMIC_DRAW)
                mesh.bind()

                for (entry in builders) {
                    shader.getUniform("Sampler0")?.setSampler(TextureUtil.INSTANCE.getMinecraftTexture(entry.key))
                    shader.getUniform("QuadStride")?.setValue(4)

                    val built = entry.value.buildOrThrow()
                    size += built.drawState().indexCount / 6
                    mesh.upload(built)

                    for (face in GlTextureCube.Face.entries) {
                        shader.getUniform("Face")?.setValue(face.ordinal)

                        mesh.draw()
                    }
                }

                mesh.unbind()
                mesh.free()
            }

            for (builder in toFree) {
                builder.close()
            }

            toFree.clear()
            builders.clear()
        }
    }
}