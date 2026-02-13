package net.typho.vibrancy.block.impl

import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.util.ExtraCodecs
import net.minecraft.world.level.block.state.StateDefinition
import net.typho.big_shot_lib.api.client.rendering.event.RenderData
import net.typho.big_shot_lib.api.client.rendering.shaders.NeoShaderRegistry
import net.typho.big_shot_lib.api.client.rendering.state.*
import net.typho.big_shot_lib.api.client.rendering.textures.GlFramebuffer
import net.typho.big_shot_lib.api.client.rendering.textures.GlTexture
import net.typho.big_shot_lib.api.util.IColor
import net.typho.vibrancy.LightManager
import net.typho.vibrancy.LightRenderResult
import net.typho.vibrancy.Vibrancy
import net.typho.vibrancy.block.BlockLightType
import net.typho.vibrancy.util.StateFunction
import org.joml.Matrix4f
import org.joml.Vector3f

object SubtleLightType : BlockLightType<SubtleLightInfo, SubtleLight, SubtleLightStorage> {
    @JvmField
    val renderSettings = RenderSettings(
        Vibrancy.id("subtle_light"),
        listOf(
            CullShard(true, CullFace.FRONT),
            BlendShard(
                true,
                IColor.from(0xFFFFFFFF.toInt()),
                BlendEquation.ADD,
                BlendFunction.Basic(
                    BlendFactor.ONE,
                    BlendFactor.ONE
                )
            )
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

    override fun render(manager: LightManager, data: RenderData, lights: SubtleLightStorage, fbo: GlFramebuffer): LightRenderResult {
        val result = LightRenderResult(numRendered = 0)

        if (Vibrancy.config.blockLights.subtle.enabled) {
            lights.checkDirty(manager)

            renderSettings.bind()

            fbo.bind()
            GlStateManager._viewport(0, 0, fbo.width(), fbo.height())

            val boxShader = NeoShaderRegistry.get(Vibrancy.id("block/subtle/box"))!!

            boxShader.bind()
            boxShader.setCommonUniforms(data)

            boxShader.getUniform("IProjMat")?.setValue(Matrix4f(Vibrancy.iProjMat))
            boxShader.getUniform("IModelMat")?.setValue(Matrix4f(Vibrancy.iModelMat))

            boxShader.getUniform("CameraPos")?.setValue(Vibrancy.camera)
            boxShader.getUniform("LightRadius")?.setValue(4f)
            boxShader.getUniform("LightBrightness")?.setValue(Vibrancy.config.blockLights.subtle.brightness.get())

            boxShader.getUniform("VibrancyWorldPosSampler")?.setSampler(Vibrancy.WORLD_POS_FBO.colorAttachments[0] as GlTexture)

            val meshes = lights.meshes.values.sortedBy { mesh -> manager.getSortingOrder(mesh.pos) }

            for (mesh in meshes) {
                if (result.numRendered!! + mesh.size > Vibrancy.config.blockLights.subtle.maxRendered.get()) {
                    break
                }

                result.add(mesh.render(data, manager))
            }

            renderSettings.unbind()
        }

        return result
    }
}