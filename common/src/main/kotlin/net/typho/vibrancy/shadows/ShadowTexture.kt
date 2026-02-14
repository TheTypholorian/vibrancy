package net.typho.vibrancy.shadows

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
import net.typho.big_shot_lib.api.util.buffers.BufferUploader
import net.typho.big_shot_lib.api.util.resources.ResourceIdentifier
import net.typho.vibrancy.util.EmptyVertexConsumer
import org.lwjgl.opengl.GL13.GL_TEXTURE_CUBE_MAP
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
    val texture by lazy {
        val texture = NeoTextureCube(TextureFormat.DEPTH_COMPONENT16)

        texture.resize(width, height).uploadNull()

        texture.bind()
        texture.setInterpolation(InterpolationType.NEAREST)
        OpenGL.INSTANCE.textureWrapping(GL_TEXTURE_CUBE_MAP, WrappingType.CLAMP_TO_EDGE, WrappingType.CLAMP_TO_EDGE, WrappingType.CLAMP_TO_EDGE)
        texture.setCompareMode(TextureComparisonMode.COMPARE_REF_TO_TEXTURE)
        texture.setCompareFunc(ComparisonFunc.LEQUAL)
        texture.unbind()

        return@lazy texture
    }
    val target by lazy {
        NeoFramebuffer(
            listOf(NeoTexture2D(TextureFormat.R8)),
            attachmentFace(GlTextureCube.Face.POS_X),
            width,
            height
        )
    }
    @JvmField
    var shadows: Collection<LightFace> = emptyList()
    @JvmField
    val toFree = LinkedList<ByteBufferBuilder>()
    @JvmField
    var size = 0

    fun attachmentFace(face: GlTextureCube.Face) : GlFramebufferAttachment {
        return object : GlFramebufferAttachment { // TODO
            override fun format() = texture.format

            override fun attachToFramebuffer(attachment: Int) {
                OpenGL.INSTANCE.attachFramebufferTexture2D(attachment, face.glId, texture.glId)
            }

            override fun resize(
                width: Int,
                height: Int
            ): BufferUploader? {
                return null//texture.resize(width, height)
            }
        }
    }

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

            target.viewport()

            shader.bind()
            uniforms.accept(shader)

            GlFlag.CULL_FACE.disable()
            GlFlag.DEPTH_TEST.enable()
            OpenGL.INSTANCE.depthFunc(ComparisonFunc.ALWAYS)
            GlFlag.BLEND.disable()

            size = 0

            if (!builders.isEmpty()) {
                val mesh = Mesh(DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.QUADS, BufferUsage.DYNAMIC_DRAW)

                for (entry in builders) {
                    shader.getUniform("Sampler0")?.setSampler(TextureUtil.INSTANCE.getMinecraftTexture(entry.key))
                    shader.getUniform("QuadStride")?.setValue(4)

                    val built = entry.value.buildOrThrow()
                    size += built.drawState().indexCount / 6
                    mesh.upload(built)
                    mesh.bind()

                    for (face in GlTextureCube.Face.entries) {
                        shader.getUniform("Face")?.setValue(face.ordinal)

                        target.depthAttachment = attachmentFace(face)
                        target.bind()
                        target.clear(ClearBit.Depth(1f))

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