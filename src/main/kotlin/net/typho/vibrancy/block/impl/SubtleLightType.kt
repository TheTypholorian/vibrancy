package net.typho.vibrancy.block.impl

import net.minecraft.util.profiling.ProfilerFiller
import net.minecraft.world.level.block.state.StateDefinition
import net.typho.big_shot_lib.api.client.rendering.opengl.constant.GlBlendEquation
import net.typho.big_shot_lib.api.client.rendering.opengl.constant.GlBlendingFactor
import net.typho.big_shot_lib.api.client.rendering.opengl.constant.GlClearBit
import net.typho.big_shot_lib.api.client.rendering.opengl.constant.GlTextureTarget
import net.typho.big_shot_lib.api.client.rendering.opengl.resource.type.GlFramebuffer
import net.typho.big_shot_lib.api.client.rendering.opengl.state.GlBlendShard
import net.typho.big_shot_lib.api.client.rendering.opengl.state.GlTextureBinding
import net.typho.big_shot_lib.api.client.rendering.opengl.util.BlendFunction
import net.typho.big_shot_lib.api.client.rendering.util.FogUtil
import net.typho.big_shot_lib.api.client.rendering.util.NeoAtlas
import net.typho.big_shot_lib.api.client.util.event.RenderEventData
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
import kotlin.collections.forEach
import kotlin.use

object SubtleLightType : BlockLightType<SubtleLightInfo, SubtleLightStorage> {
    override fun infoCodec(stateDefinition: StateDefinition<*, *>) = SubtleLightInfo.codec(stateDefinition)

    override fun castInfo(info: Any?): SubtleLightInfo? {
        return info as? SubtleLightInfo
    }

    override fun createStorage(manager: LightManager) = SubtleLightStorage()

    override fun render(
        manager: LightManager,
        result: GlFramebuffer,
        temp: GlFramebuffer,
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
            temp.bind().use { fbo ->
                profiler.push("clear")
                fbo.clear(GlClearBit.Color(NeoColor.FULL_OFF))
                profiler.pop()

                LightMesh.drawState(NeoAtlas.blocks, Vibrancy.id("block/subtle/mesh"), blend = GlBlendShard.Enabled(
                    BlendFunction.Basic(
                        GlBlendingFactor.ONE,
                        GlBlendingFactor.ONE
                    ),
                    GlBlendEquation.MAX
                )).bind().use { settings ->
                    profiler.push("uniforms")
                    settings.shader.setTexture(1, GlTextureBinding.FromInstance(
                        ReflectionAtlases[NeoIdentifier("blocks")], //NeoAtlas.blocks.location
                        GlTextureTarget.TEXTURE_2D
                    ))
                    settings.shader.setUniform("ProjMat") { set(data.projMat) }
                    settings.shader.setUniform("ModelViewMat") { set(data.modelViewMat.translate((-data.camera.pos).toJOML(), Matrix4f())) }
                    settings.shader.setUniform("LightBrightness") { set(VibrancyConfig.subtleLightBrightness) }
                    FogUtil.INSTANCE.upload(settings.shader)
                    profiler.pop()

                    profiler.push("sort")
                    val distance = manager.getRenderDistance(VibrancyConfig.subtleLightsRenderDistance).toFloat()
                    val chunks = lights.chunks.values
                        .filter {
                            manager.inRenderDistance(data, it.pos, distance)
                        }
                    profiler.pop()

                    profiler.push("render")
                    chunks.forEach {
                        it.render(manager, data, settings.shader, debugOut, profiler)
                    }
                    profiler.pop()
                }

                profiler.push("blit")
                manager.blitFromTemp(result, temp)
                profiler.pop()
            }
            profiler.pop()
        }
    }
}