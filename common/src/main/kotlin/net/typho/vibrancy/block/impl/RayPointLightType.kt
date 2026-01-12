package net.typho.vibrancy.block.impl

import com.mojang.blaze3d.vertex.VertexBuffer
import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.client.Minecraft
import net.minecraft.util.ExtraCodecs
import net.minecraft.world.level.block.state.StateDefinition
import net.typho.big_shot_lib.api.impl.NeoShader
import net.typho.big_shot_lib.gl.GlStack
import net.typho.big_shot_lib.gl.state.*
import net.typho.vibrancy.LightManager
import net.typho.vibrancy.Vibrancy
import net.typho.vibrancy.VibrancyDynamicBuffers
import net.typho.vibrancy.block.BlockLightType
import net.typho.vibrancy.block.RenderingBlockLight
import net.typho.vibrancy.util.StateFunction
import org.joml.Matrix4f
import org.lwjgl.opengl.GL11.GL_STENCIL_BUFFER_BIT
import org.lwjgl.opengl.GL11.glClear

object RayPointLightType : BlockLightType<RayPointLightInfo, RayPointLight> {
    override fun codec(stateDefinition: StateDefinition<*, *>): MapCodec<RayPointLightInfo> {
        return RecordCodecBuilder.mapCodec {
            it.group(
                StateFunction.Companion.codec(ExtraCodecs.VECTOR3F, stateDefinition)
                    .fieldOf("color")
                    .forGetter { info -> info.color },
                StateFunction.Companion.codec(Codec.FLOAT, stateDefinition)
                    .fieldOf("radius")
                    .forGetter { info -> info.radius },
                StateFunction.Companion.codec(Codec.FLOAT, stateDefinition)
                    .fieldOf("brightness")
                    .forGetter { info -> info.brightness },
                StateFunction.Companion.codec(ExtraCodecs.VECTOR3F, stateDefinition)
                    .fieldOf("offset")
                    .forGetter { info -> info.offset },
                StateFunction.Companion.codec(Codec.BOOL, stateDefinition)
                    .optionalFieldOf("enabled", StateFunction(true))
                    .forGetter { info -> info.enabled }
            ).apply(it, ::RayPointLightInfo)
        }
    }

    override fun render(manager: LightManager, lights: Set<RenderingBlockLight<RayPointLight>>) {
        GlStack().use { stack ->
            stack.disable(GlCapability.DEPTH_TEST)
            stack.enable(GlCapability.STENCIL_TEST)
            stack.enable(GlCapability.CULL_FACE)
            stack.set(CullFace.FRONT)
            stack.enable(GlCapability.BLEND)
            stack.set(
                BlendFunction(
                    BlendFactor.ONE,
                    BlendFactor.ONE
                )
            )
            stack.set(StencilMask, LightManager.SHADOW_STENCIL_MASK)
            stack.set(StencilFunc(
                ComparisonMode.NOTEQUAL,
                LightManager.SHADOW_STENCIL_MASK,
                LightManager.SHADOW_STENCIL_MASK
            ))

            lights.stream()
                .sorted(Comparator.comparingDouble { manager.getSortingOrder(it.light) })
                .forEachOrdered { light ->
                    if (light.light.shadowsDirty) {
                        light.light.rebuildShadows(manager)
                    }

                    light.light.shadows.checkIfFinished()

                    glClear(GL_STENCIL_BUFFER_BIT)

                    val shadowShader = NeoShader.get(Vibrancy.id("point_shadow"))!!

                    shadowShader.bind(stack)
                    shadowShader.setCommonUniforms(modelViewMat = manager.getViewMatrix())

                    shadowShader.getUniform("IProjMat")?.set(Matrix4f(Vibrancy.iProjMat))
                    shadowShader.getUniform("IModelMat")?.set(Matrix4f(Vibrancy.iModelMat))

                    shadowShader.getUniform("LightPos")?.set(light.light.getAbsolutePos())
                    shadowShader.getUniform("LightColor")?.set(light.light.color)
                    shadowShader.getUniform("LightRadius")?.set(light.light.radius)
                    shadowShader.getUniform("CameraPos")?.set(Vibrancy.camera)

                    shadowShader.setSampler("DiffuseDepthSampler", Minecraft.getInstance().mainRenderTarget.depthTextureId)

                    stack.set(StencilOp(
                        IntAction.KEEP,
                        IntAction.KEEP,
                        IntAction.REPLACE,
                    ))

                    light.light.shadows.render(shadowShader)

                    val boxShader = NeoShader.get(Vibrancy.id("point_box"))!!

                    boxShader.bind(stack)
                    boxShader.setCommonUniforms(modelViewMat = manager.getViewMatrix())

                    boxShader.getUniform("IProjMat")?.set(Matrix4f(Vibrancy.iProjMat))
                    boxShader.getUniform("IModelMat")?.set(Matrix4f(Vibrancy.iModelMat))

                    boxShader.getUniform("LightPos")?.set(light.light.getAbsolutePos())
                    boxShader.getUniform("LightColor")?.set(light.light.color)
                    boxShader.getUniform("LightRadius")?.set(light.light.radius)
                    boxShader.getUniform("CameraPos")?.set(Vibrancy.camera)

                    boxShader.setSampler("VibrancyNormalSampler", VibrancyDynamicBuffers.normalsTexture!!)
                    boxShader.setSampler("DiffuseDepthSampler", Minecraft.getInstance().mainRenderTarget.depthTextureId)

                    stack.set(StencilOp(
                        IntAction.KEEP,
                        IntAction.KEEP,
                        IntAction.KEEP,
                    ))

                    light.light.box.bind()
                    light.light.box.draw()
                    VertexBuffer.unbind()
                }
        }
    }
}