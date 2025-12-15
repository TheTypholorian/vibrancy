package net.typho.vibrancy

import com.mojang.blaze3d.vertex.VertexBuffer
import net.irisshaders.iris.pipeline.VanillaRenderingPipeline
import net.typho.big_shot_lib.BigShotLib
import net.typho.big_shot_lib.api.ITexture
import net.typho.big_shot_lib.api.impl.NeoShader
import net.typho.big_shot_lib.gl.resource.GlResourceType
import net.typho.vibrancy.Vibrancy.DIRTY_BLOCKS
import net.typho.vibrancy.Vibrancy.LIGHT_MANAGER
import net.typho.vibrancy.Vibrancy.OUTPUT_FBO
import net.typho.vibrancy.Vibrancy.id
import net.typho.vibrancy.light.BlockLight
import net.typho.vibrancy.util.glClear
import org.lwjgl.opengl.GL11.*

open class VibrancyRenderingPipeline : VanillaRenderingPipeline() {
    open fun renderLights() {
        LIGHT_MANAGER.viewMatrix = LIGHT_MANAGER.getViewMatrix()
        LIGHT_MANAGER.lightsRendered = 0
        LIGHT_MANAGER.lightsRaytraced = 0

        OUTPUT_FBO.glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT or GL_STENCIL_BUFFER_BIT)
        OUTPUT_FBO.bind().use {
            LIGHT_MANAGER.setupStencil(id("output"))

            glColorMask(true, true, true, true)
            glDisable(GL_DEPTH_TEST)
            glCullFace(GL_FRONT)
            glEnable(GL_BLEND)
            glBlendFunc(GL_ONE, GL_ONE)
            glEnable(GL_STENCIL_TEST)
            glStencilMask(1)
            glStencilFunc(GL_ALWAYS, 0, 0xFF)
            glStencilOp(GL_KEEP, GL_KEEP, GL_KEEP)

            BlockLight.LIGHTS.values.stream()
                .sorted(Comparator.comparingDouble {
                    it.getPosition().distanceSquared(LIGHT_MANAGER.getCamera().position.toVector3f()).toDouble()
                })
                .forEachOrdered { light ->
                    if (LIGHT_MANAGER.shouldRender(light)) {
                        val raytrace = LIGHT_MANAGER.shouldRaytrace(light)

                        light.render(LIGHT_MANAGER, raytrace)
                        LIGHT_MANAGER.postRender(light, raytrace)
                    }
                }

            GlResourceType.SHADER_STORAGE_BUFFER.unbindBase(0)

            glDisable(GL_STENCIL_TEST)
        }

        // TODO albedo
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        NeoShader.get(id("post"))!!.bind().use {
            val shader = it.resource()
            shader.setCommonUniforms()
            shader.setSampler("VibrancyOutputSampler", OUTPUT_FBO.colorAttachments[0] as ITexture)

            BigShotLib.SCREEN_VBO.bind()
            BigShotLib.SCREEN_VBO.draw()
        }

        VertexBuffer.unbind()
        glEnable(GL_DEPTH_TEST)
        glCullFace(GL_BACK)
        DIRTY_BLOCKS.clear()
    }
}