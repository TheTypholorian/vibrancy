package net.typho.vibrancy.block.impl

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.util.ExtraCodecs
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
import net.typho.vibrancy.util.StateFunction
import org.joml.Matrix4f
import org.joml.Vector3f

object SubtleLightType : BlockLightType<SubtleLightInfo, SubtleLight, SubtleLightStorage> {
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

    override fun infoCodec(stateDefinition: StateDefinition<*, *>): MapCodec<SubtleLightInfo> {
        return RecordCodecBuilder.mapCodec {
            it.group(
                StateFunction.codec(ExtraCodecs.VECTOR3F, stateDefinition)
                    .fieldOf("color")
                    .forGetter { info -> info.color },
                StateFunction.codec(Codec.FLOAT, stateDefinition)
                    .fieldOf("brightness")
                    .forGetter { info -> info.brightness },
                StateFunction.codec(ExtraCodecs.VECTOR3F, stateDefinition)
                    .optionalFieldOf("offset", StateFunction(Vector3f(0.5f)))
                    .forGetter { info -> info.offset },
                StateFunction.codec(Codec.BOOL, stateDefinition)
                    .optionalFieldOf("enabled", StateFunction(true))
                    .forGetter { info -> info.enabled }
            ).apply(it, ::SubtleLightInfo)
        }
    }

    override fun createStorage(manager: LightManager) = SubtleLightStorage()

    override fun render(manager: LightManager, data: RenderEventData, lights: SubtleLightStorage, fbo: GlFramebuffer): LightRenderResult {
        val result = LightRenderResult(numRendered = 0)

        if (Vibrancy.config.blockLights.subtle.enabled) {
            lights.checkDirty(manager)

            val renderSettings = renderSettings(fbo, data)

            renderSettings.bind()

            val meshes = lights.meshes.values.sortedBy { mesh -> manager.getSortingOrder(data, mesh.pos) }

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