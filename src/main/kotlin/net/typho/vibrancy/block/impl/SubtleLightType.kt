package net.typho.vibrancy.block.impl

import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.util.profiling.ProfilerFiller
import net.minecraft.world.level.block.state.StateDefinition
import net.typho.big_shot_lib.api.client.rendering.opengl.constant.GlBlendEquation
import net.typho.big_shot_lib.api.client.rendering.opengl.constant.GlBlendingFactor
import net.typho.big_shot_lib.api.client.rendering.opengl.constant.GlClearBit
import net.typho.big_shot_lib.api.client.rendering.opengl.constant.GlTextureFormat
import net.typho.big_shot_lib.api.client.rendering.opengl.constant.GlTextureTarget
import net.typho.big_shot_lib.api.client.rendering.opengl.resource.type.GlTexture2D
import net.typho.big_shot_lib.api.client.rendering.opengl.state.GlBlendShard
import net.typho.big_shot_lib.api.client.rendering.opengl.state.GlDrawState
import net.typho.big_shot_lib.api.client.rendering.opengl.state.GlShaderShard
import net.typho.big_shot_lib.api.client.rendering.opengl.state.GlTextureBinding
import net.typho.big_shot_lib.api.client.rendering.opengl.util.BlendFunction
import net.typho.big_shot_lib.api.client.rendering.util.Mesh
import net.typho.big_shot_lib.api.client.rendering.util.NeoAtlas
import net.typho.big_shot_lib.api.client.util.event.RenderEventData
import net.typho.big_shot_lib.api.math.rect.NeoRect2i
import net.typho.big_shot_lib.api.math.vec.IVec3.Companion.toJOML
import net.typho.big_shot_lib.api.util.NeoColor
import net.typho.big_shot_lib.api.util.resource.NeoIdentifier
import net.typho.vibrancy.LightManager
import net.typho.vibrancy.Vibrancy
import net.typho.vibrancy.VibrancyConfig
import net.typho.vibrancy.block.BlockLightType
import net.typho.vibrancy.shadows.LightMesh
import net.typho.vibrancy.util.ReflectionAtlases
import org.joml.Matrix4f
import kotlin.use

object SubtleLightType : BlockLightType<SubtleLightInfo, SubtleLightStorage> {
    @JvmField
    val meshBlitDrawState = GlDrawState.Basic(
        shader = GlShaderShard.FromLocation(
            Vibrancy.id("block/subtle/blit"),
            { }
        )
    )

    override fun infoCodec(stateDefinition: StateDefinition<*, *>) = SubtleLightInfo.codec(stateDefinition)

    override fun castInfo(info: Any?): SubtleLightInfo? {
        return info as? SubtleLightInfo
    }

    override fun createStorage(manager: LightManager) = SubtleLightStorage()

    override fun render(
        manager: LightManager,
        data: RenderEventData,
        lights: SubtleLightStorage,
        debugOut: (String, Int) -> Unit,
        profiler: ProfilerFiller
    ) {
        if (VibrancyConfig.subtleLightsEnabled) {
            profiler.push("checkDirty")
            lights.checkDirty(manager, data, profiler)
            profiler.pop()

            profiler.push("render")

            val targetAttachment = data.target.colorAttachments[0] as GlTexture2D
            val width = targetAttachment.width.coerceAtLeast(1)
            val height = targetAttachment.height.coerceAtLeast(1)

            lights.framebuffer.bind().use { fbo ->
                if (lights.target.width != width || lights.target.height != height) {
                    lights.target.bind(GlTextureTarget.TEXTURE_2D).use {
                        it.textureDataMutable(width, height, GlTextureFormat.RGB16F)
                    }
                }

                fbo.depthAttachment = data.target.depthAttachment
                fbo.checkStatus().throwIfError()

                lights.framebuffer.bind(NeoRect2i(0, 0, width, height)).use { fbo ->
                    fbo.clear(GlClearBit.Color(NeoColor.FULL_OFF))

                    LightMesh.drawState(NeoAtlas.blocks, Vibrancy.id("block/subtle/mesh"), lightLimited = true).bind().use { settings ->
                        settings.shader.setTexture(1, GlTextureBinding.FromInstance(
                            ReflectionAtlases[NeoIdentifier("blocks")], //NeoAtlas.blocks.location
                            GlTextureTarget.TEXTURE_2D
                        ))
                        settings.shader.setUniform("ProjMat") { set(data.projMat) }
                        settings.shader.setUniform("ModelViewMat") { set(data.modelViewMat.translate((-data.camera.pos).toJOML(), Matrix4f())) }
                        settings.shader.setUniform("LightBrightness") { set(VibrancyConfig.subtleLightBrightness) }

                        settings.shader.setUniform("FogStart") { set(RenderSystem.getShaderFogStart()) }
                        settings.shader.setUniform("FogEnd") { set(RenderSystem.getShaderFogEnd()) }
                        settings.shader.setUniform("FogShape") { set(RenderSystem.getShaderFogShape().index) }

                        lights.chunks.values
                            .filter {
                                manager.inRenderDistance(data, it.pos, VibrancyConfig.subtleLightsRenderDistance)
                            }
                            .forEach {
                                it.render(manager, data, settings.shader, debugOut)
                            }
                    }
                }
            }
            profiler.pop()

            profiler.push("blit")
            val drawState = GlDrawState.Basic(
                blend = GlBlendShard.Enabled(
                    BlendFunction.Basic(
                        GlBlendingFactor.ONE,
                        GlBlendingFactor.ONE
                    ),
                    GlBlendEquation.ADD
                ),
                shader = GlShaderShard.FromLocation(
                    Vibrancy.id("light_post"),
                    {},
                    GlTextureBinding.FromInstance(lights.target, GlTextureTarget.TEXTURE_2D)
                )
            )
            drawState.bind().use { Mesh.SCREEN_MESH.draw() }
            profiler.pop()
        }
    }
}