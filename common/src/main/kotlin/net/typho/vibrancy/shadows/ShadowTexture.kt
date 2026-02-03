package net.typho.vibrancy.shadows

import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.vertex.*
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.resources.ResourceLocation
import net.typho.big_shot_lib.BigShotLib
import net.typho.big_shot_lib.api.IShader
import net.typho.big_shot_lib.api.ITexture
import net.typho.big_shot_lib.api.builtin.EmptyVertexConsumer
import net.typho.big_shot_lib.api.impl.NeoFramebuffer
import net.typho.big_shot_lib.api.impl.NeoIndexedBuffer
import net.typho.big_shot_lib.gl.GlStack
import net.typho.big_shot_lib.gl.InterpolationType
import net.typho.big_shot_lib.gl.resource.BufferUsage
import net.typho.big_shot_lib.gl.resource.GlResourceType
import net.typho.big_shot_lib.gl.resource.TextureFormat
import net.typho.big_shot_lib.gl.state.*
import net.typho.vibrancy.Vibrancy
import org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT
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
        val fbo = NeoFramebuffer.TextureBacked(
            Vibrancy.id("shadow_texture_${width}x${height}"),
            arrayOf(TextureFormat.R16F),
            null,
            width,
            height
        )
        (fbo.colorAttachments[0] as ITexture).setInterpolation(InterpolationType.LINEAR)
        fbo
    }
    @JvmField
    var shadows: Collection<LightFace> = emptyList()
    @JvmField
    val toFree = LinkedList<ByteBufferBuilder>()
    @JvmField
    var size = 0

    override fun free() {
        target.release()
    }

    fun getRenderTypeTexture(renderType: RenderType): ResourceLocation? {
        return when (renderType) {
            is RenderType.CompositeRenderType -> {
                renderType.state().textureState.cutoutTexture().getOrNull()
            }
            else -> null
        }
    }

    fun begin(shader: IShader, uniforms: Consumer<IShader>) = Builder(shader, uniforms)

    inner class Builder(val shader: IShader, val uniforms: Consumer<IShader>) : MultiBufferSource {
        val builders = HashMap<ResourceLocation, ShadowBufferBuilder>()

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
                builders.computeIfAbsent(texture) {
                    val builder = ByteBufferBuilder(renderType.bufferSize())
                    toFree.add(builder)
                    ShadowBufferBuilder(builder)
                }
            }
        }

        fun finish() {
            GlStack().use { stack ->
                target.bind(stack)

                GlStateManager._viewport(0, 0, target.width(), target.height())
                GlStateManager._clearColor(1f, 1f, 1f, 1f)
                GlStateManager._clear(GL_COLOR_BUFFER_BIT, false)

                shader.bind(stack)
                uniforms.accept(shader)

                stack.enable(GlCapability.BLEND)
                stack.set(BlendEquation.MIN)
                stack.disable(GlCapability.DEPTH_TEST) // TODO
                stack.set(DepthTest, ComparisonMode.LEQUAL)
                stack.set(ColorMask(true, true, true, true))
                stack.set(DepthMask, true)
                stack.disable(GlCapability.CULL_FACE)

                size = 0

                if (!builders.isEmpty()) {
                    val vbo = BigShotLib.SCREEN_VBO
                    vbo.bind()

                    val ssbo = NeoIndexedBuffer(null, GlResourceType.SHADER_STORAGE_BUFFER, BufferUsage.STREAM_DRAW)
                    ssbo.bind()
                    ssbo.bindBase(stack, 0)

                    for (entry in builders) {
                        size += entry.value.numQuads()

                        shader.setSampler("Sampler0", Minecraft.getInstance().textureManager.getTexture(entry.key))
                        ssbo.upload(entry.value.build())

                        vbo.draw()
                    }

                    ssbo.unbind()
                    VertexBuffer.unbind()
                }

                for (builder in toFree) {
                    builder.close()
                }

                toFree.clear()
                builders.clear()
            }
        }
    }
}