package net.typho.vibrancy

import com.mojang.blaze3d.vertex.VertexBuffer
import net.irisshaders.iris.gl.texture.DepthBufferFormat
import net.irisshaders.iris.pbr.TextureInfoCache
import net.irisshaders.iris.pipeline.VanillaRenderingPipeline
import net.irisshaders.iris.shaderpack.properties.PackDirectives
import net.irisshaders.iris.shaderpack.properties.PackRenderTargetDirectives
import net.irisshaders.iris.shaderpack.properties.ShaderProperties
import net.irisshaders.iris.targets.Blaze3dRenderTargetExt
import net.irisshaders.iris.targets.RenderTargets
import net.minecraft.client.Minecraft
import net.typho.big_shot_lib.BigShotLib
import net.typho.big_shot_lib.api.ITexture
import net.typho.big_shot_lib.api.impl.NeoShader
import net.typho.big_shot_lib.gl.GlStack
import net.typho.big_shot_lib.gl.resource.GlResourceType
import net.typho.big_shot_lib.gl.state.*
import net.typho.vibrancy.Vibrancy.DIRTY_BLOCKS
import net.typho.vibrancy.Vibrancy.LIGHT_MANAGER
import net.typho.vibrancy.Vibrancy.OUTPUT_FBO
import net.typho.vibrancy.Vibrancy.id
import net.typho.vibrancy.light.BlockLight
import org.lwjgl.opengl.GL11.*

open class VibrancyRenderingPipeline : VanillaRenderingPipeline() {
    val renderTargets: RenderTargets

    init {
        val main = Minecraft.getInstance().mainRenderTarget
        val directives = PackDirectives(PackRenderTargetDirectives.BASELINE_SUPPORTED_RENDER_TARGETS, ShaderProperties.empty())
        renderTargets = RenderTargets(
            main.width,
            main.height,
            main.depthTextureId,
            (main as Blaze3dRenderTargetExt).`iris$getDepthBufferVersion`(),
            DepthBufferFormat.fromGlEnumOrDefault(TextureInfoCache.INSTANCE.getInfo(main.depthTextureId).internalFormat),
            directives.renderTargetDirectives.renderTargetSettings,
            directives
        )
    }

    open fun renderLights() {
        LIGHT_MANAGER.viewMatrix = LIGHT_MANAGER.getViewMatrix()
        LIGHT_MANAGER.lightsRendered = 0
        LIGHT_MANAGER.lightsRaytraced = 0

        GlStack().use { stack ->
            OUTPUT_FBO.bind(stack)

            glClearColor(0f, 0f, 0f, 0f)
            glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT or GL_STENCIL_BUFFER_BIT)

            LIGHT_MANAGER.setupStencil(stack)

            stack.set(ColorMask, ColorMask.Mask(true, true, true, true))
            stack.disable(GlCapability.DEPTH_TEST)
            stack.set(CullFace, Face.FRONT)
            stack.enable(GlCapability.BLEND)
            stack.set(
                BlendFunction, BlendFunction.Mode(
                    BlendFunction.Factor.ONE,
                    BlendFunction.Factor.ONE
                )
            )
            stack.set(StencilMask, 1)
            stack.set(
                StencilFunc, StencilFunc.Mode(
                    ComparisonMode.ALWAYS,
                    0,
                    0xFF
                )
            )
            stack.set(
                StencilOp, StencilOp.Mode(
                    IntAction.KEEP,
                    IntAction.KEEP,
                    IntAction.KEEP
                )
            )

            BlockLight.LIGHTS.values.stream()
                .sorted(Comparator.comparingDouble {
                    it.getPosition().distanceSquared(LIGHT_MANAGER.getCamera().position.toVector3f()).toDouble()
                })
                .forEachOrdered { light ->
                    if (LIGHT_MANAGER.shouldRender(light)) {
                        val raytrace = LIGHT_MANAGER.shouldRaytrace(light)

                        light.render(LIGHT_MANAGER, raytrace, stack)
                        LIGHT_MANAGER.postRender(light, raytrace)
                    }
                }

            stack.boundMap[GlResourceType.FRAMEBUFFER]?.unbind()

            // TODO albedo
            stack.disable(GlCapability.DEPTH_TEST)
            stack.disable(GlCapability.CULL_FACE)
            stack.enable(GlCapability.BLEND)
            stack.set(
                BlendFunction, BlendFunction.Mode(
                    BlendFunction.Factor.ONE,
                    BlendFunction.Factor.ONE
                )
            )
            val shader = NeoShader.get(id("post"))!!
            shader.bind(stack)
            shader.setCommonUniforms()
            shader.setSampler("VibrancyOutputSampler", OUTPUT_FBO.colorAttachments[0] as ITexture)

            BigShotLib.SCREEN_VBO.bind()
            BigShotLib.SCREEN_VBO.draw()

            DIRTY_BLOCKS.clear()
        }

        VertexBuffer.unbind()
    }
}