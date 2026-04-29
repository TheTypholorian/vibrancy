package net.typho.vibrancy.block.impl

import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.util.profiling.ProfilerFiller
import net.minecraft.world.level.block.state.StateDefinition
import net.typho.big_shot_lib.api.client.rendering.opengl.state.GlDrawState
import net.typho.big_shot_lib.api.client.rendering.opengl.state.GlShaderShard
import net.typho.big_shot_lib.api.client.rendering.util.NeoAtlas
import net.typho.big_shot_lib.api.client.util.event.RenderEventData
import net.typho.big_shot_lib.api.math.vec.IVec3.Companion.toJOML
import net.typho.vibrancy.LightManager
import net.typho.vibrancy.Vibrancy
import net.typho.vibrancy.VibrancyConfig
import net.typho.vibrancy.block.BlockLightType
import net.typho.vibrancy.shadows.LightMesh
import org.joml.Matrix4f

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
            LightMesh.drawState(NeoAtlas.blocks, Vibrancy.id("block/subtle/mesh")).bind().use { settings ->
                settings.shader.setUniform("ProjMat") { set(data.projMat) }
                settings.shader.setUniform("ModelViewMat") { set(data.modelViewMat.translate((-data.camera.pos).toJOML(), Matrix4f())) }
                settings.shader.setUniform("LightBrightness") { set(VibrancyConfig.subtleLightBrightness) }
                settings.shader.setUniform("CameraPos") { setFloatVec(data.camera.pos) }

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
            profiler.pop()
        }
    }
}