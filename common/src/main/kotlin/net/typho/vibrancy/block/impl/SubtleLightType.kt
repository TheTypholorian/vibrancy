package net.typho.vibrancy.block.impl

import net.minecraft.world.level.block.state.StateDefinition
import net.typho.big_shot_lib.api.client.opengl.buffers.GlFramebuffer
import net.typho.big_shot_lib.api.client.opengl.buffers.GlTexture
import net.typho.big_shot_lib.api.client.opengl.state.*
import net.typho.big_shot_lib.api.client.util.events.RenderEventData
import net.typho.big_shot_lib.api.util.IColor
import net.typho.vibrancy.LightManager
import net.typho.vibrancy.LightRenderResult
import net.typho.vibrancy.Vibrancy
import net.typho.vibrancy.block.BlockLightType
import org.joml.Matrix4f

object SubtleLightType : BlockLightType<SubtleLightInfo, SubtleLightStorage> {
    @JvmStatic
    fun renderSettings(fbo: GlFramebuffer, data: RenderEventData) = RenderSettings(
        Vibrancy.id("subtle_light"),
        listOf(
            CullShard(true, CullFace.FRONT),
            BlendShard(
                true,
                IColor.FULL_ON,
                BlendEquation.ADD,
                BlendFunction.Basic(BlendFactor.ONE, BlendFactor.ONE)
            ),
            FramebufferShard(
                { fbo },
                true
            ),
            ShaderShard(
                Vibrancy.id("block/subtle/box")
            ) { shader ->
                shader.setCommonUniforms(data)

                shader.getUniform("IProjMat")?.setValue(Matrix4f(data.inverseProjMat))
                shader.getUniform("IModelMat")?.setValue(Matrix4f(data.inverseModelViewMat))

                shader.getUniform("CameraPos")?.setValue(data.camera.pos)
                shader.getUniform("LightRadius")?.setValue(4f)
                shader.getUniform("LightBrightness")?.setValue(Vibrancy.config.blockLights.subtle.brightness)

                shader.getUniform("VibrancyWorldPosSampler")?.setSampler(Vibrancy.worldPosFbo.colorAttachments[0] as GlTexture)
            }
        )
    )

    override fun infoCodec(stateDefinition: StateDefinition<*, *>) = SubtleLightInfo.codec(stateDefinition)

    override fun castInfo(info: Any?): SubtleLightInfo? {
        return info as? SubtleLightInfo
    }

    override fun createStorage(manager: LightManager) = SubtleLightStorage()

    override fun render(manager: LightManager, data: RenderEventData, lights: SubtleLightStorage, fbo: GlFramebuffer): LightRenderResult {
        val result = LightRenderResult(numRendered = 0)

        if (Vibrancy.config.blockLights.subtle.enabled) {
            lights.checkDirty(manager)

            val renderSettings = renderSettings(fbo, data)

            renderSettings.bind()

            val meshes = lights.chunks.values.sortedBy { manager.getSortingOrder(data, it.pos) }

            for (mesh in meshes) {
                if ((result.numRendered ?: 0) + mesh.size > Vibrancy.config.blockLights.subtle.maxRendered) {
                    break
                }

                result.add(mesh.render(data, manager))
            }

            renderSettings.unbind()
        }

        return result
    }
}