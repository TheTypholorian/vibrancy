package net.typho.vibrancy.block.impl

import com.mojang.blaze3d.vertex.VertexBuffer
import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.util.ExtraCodecs
import net.minecraft.world.level.block.state.StateDefinition
import net.typho.big_shot_lib.api.IFramebuffer
import net.typho.big_shot_lib.api.ITexture
import net.typho.big_shot_lib.api.impl.NeoShader
import net.typho.big_shot_lib.gl.GlStack
import net.typho.big_shot_lib.gl.state.BlendFactor
import net.typho.big_shot_lib.gl.state.BlendFunction
import net.typho.big_shot_lib.gl.state.CullFace
import net.typho.big_shot_lib.gl.state.GlCapability
import net.typho.vibrancy.LightManager
import net.typho.vibrancy.LightRenderResult
import net.typho.vibrancy.Vibrancy
import net.typho.vibrancy.block.BlockLightType
import net.typho.vibrancy.util.StateFunction
import org.joml.Matrix4f
import org.joml.Vector3f

object SubtleLightType : BlockLightType<SubtleLightInfo, SubtleLight, SubtleLightStorage> {
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

    override fun render(manager: LightManager, lights: SubtleLightStorage, fbo: IFramebuffer): LightRenderResult {
        val result = LightRenderResult(numRendered = 0)

        if (Vibrancy.config.blockLights.subtle.enabled) {
            lights.checkDirty(manager)

            GlStack().use { stack ->
                stack.disable(GlCapability.DEPTH_TEST)
                //stack.disable(GlCapability.STENCIL_TEST)
                stack.enable(GlCapability.CULL_FACE)
                stack.set(CullFace.FRONT)
                stack.enable(GlCapability.BLEND)
                stack.set(
                    BlendFunction(
                        BlendFactor.ONE,
                        BlendFactor.ONE
                    )
                )

                val boxShader = NeoShader.get(Vibrancy.id("block/subtle/box"))!!

                boxShader.bind(stack)
                boxShader.setCommonUniforms(modelViewMat = manager.getViewMatrix())

                boxShader.getUniform("IProjMat")?.set(Matrix4f(Vibrancy.iProjMat))
                boxShader.getUniform("IModelMat")?.set(Matrix4f(Vibrancy.iModelMat))

                boxShader.getUniform("CameraPos")?.set(Vibrancy.camera)
                boxShader.getUniform("LightRadius")?.set(4f)
                boxShader.getUniform("LightBrightness")?.set(Vibrancy.config.blockLights.subtle.brightness.get())

                boxShader.setSampler("VibrancyWorldPosSampler", Vibrancy.WORLD_POS_FBO.colorAttachments[0] as ITexture)

                val meshes = lights.meshes.values.sortedBy { mesh -> manager.getSortingOrder(mesh.pos) }

                for (mesh in meshes) {
                    if (result.numRendered!! + mesh.size > Vibrancy.config.blockLights.subtle.maxRendered.get()) {
                        break
                    }

                    result.add(mesh.render(manager, stack))
                }

                VertexBuffer.unbind()
            }
        }

        return result
    }
}