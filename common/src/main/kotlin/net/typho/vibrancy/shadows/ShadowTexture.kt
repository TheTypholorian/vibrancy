package net.typho.vibrancy.shadows

import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.vertex.*
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.resources.ResourceLocation
import net.typho.big_shot_lib.api.IShader
import net.typho.big_shot_lib.api.builtin.EmptyVertexConsumer
import net.typho.big_shot_lib.api.impl.NeoFramebuffer
import net.typho.big_shot_lib.gl.GlStack
import net.typho.big_shot_lib.gl.resource.TextureFormat
import net.typho.big_shot_lib.gl.state.ColorMask
import net.typho.big_shot_lib.gl.state.ComparisonMode
import net.typho.big_shot_lib.gl.state.CullFace
import net.typho.big_shot_lib.gl.state.DepthMask
import net.typho.big_shot_lib.gl.state.DepthTest
import net.typho.big_shot_lib.gl.state.GlCapability
import net.typho.vibrancy.Vibrancy
import org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT
import org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT
import org.lwjgl.system.NativeResource
import java.util.LinkedList
import java.util.function.Consumer
import kotlin.jvm.optionals.getOrNull

open class ShadowTexture(
    @JvmField
    val width: Int,
    @JvmField
    val height: Int
) : NativeResource {
    val target by lazy {
        NeoFramebuffer.TextureBacked(
            Vibrancy.id("shadow_texture_${width}x${height}"),
            arrayOf(TextureFormat.RGB8),
            TextureFormat.DEPTH_COMPONENT,
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
        val builders = HashMap<ResourceLocation, BufferBuilder>()

        override fun getBuffer(renderType: RenderType): VertexConsumer {
            val texture = getRenderTypeTexture(renderType)

            return if (
                texture == null
                || renderType.mode().primitiveLength < 3
                || !renderType.format().contains(VertexFormatElement.POSITION)
                || !renderType.format().contains(VertexFormatElement.UV0)
            ) {
                EmptyVertexConsumer
            } else {
                builders.computeIfAbsent(texture) {
                    val builder = ByteBufferBuilder(renderType.bufferSize())
                    toFree.add(builder)
                    BufferBuilder(builder, renderType.mode(), DefaultVertexFormat.POSITION_TEX)
                }
            }
        }

        fun finish() {
            GlStack().use { stack ->
                target.bind(stack)

                GlStateManager._clearColor(0f, 0f, 0f, 0f)
                GlStateManager._clearDepth(1.0)
                GlStateManager._clear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT, false)

                shader.bind(stack)
                uniforms.accept(shader)

                stack.disable(GlCapability.BLEND)
                stack.enable(GlCapability.DEPTH_TEST)
                stack.set(DepthTest, ComparisonMode.LEQUAL)
                stack.set(ColorMask(true, true, true, true))
                stack.set(DepthMask, true)
                stack.enable(GlCapability.CULL_FACE)
                stack.set(CullFace.BACK)

                if (!builders.isEmpty()) {
                    val vbo = VertexBuffer(VertexBuffer.Usage.DYNAMIC)
                    vbo.bind()

                    for (entry in builders) {
                        shader.setSampler("Sampler0", Minecraft.getInstance().textureManager.getTexture(entry.key))

                        vbo.upload(entry.value.buildOrThrow())
                        vbo.draw()
                    }

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