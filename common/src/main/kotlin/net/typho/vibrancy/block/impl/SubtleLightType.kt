package net.typho.vibrancy.block.impl

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
        debugOut: (String, Int) -> Unit
    ) {
        if (VibrancyConfig.BlockLights.Subtle.enabled) {
            lights.checkDirty(manager, data)

            LightMesh.drawState(NeoAtlas.blocks, Vibrancy.id("block/subtle/mesh")).bind().use { settings ->
                settings.shader.setUniform("ProjMat") { set(data.projMat) }
                settings.shader.setUniform("ModelViewMat") { set(data.modelViewMat.translate((-data.camera.pos).toJOML(), Matrix4f())) }
                settings.shader.setUniform("LightBrightness") { set(VibrancyConfig.BlockLights.Subtle.brightness) }

                lights.chunks.values
                    .filter {
                        manager.inRenderDistance(data, it.pos, VibrancyConfig.BlockLights.Subtle.renderDistance)
                    }
                    .forEach {
                        it.render(data, settings.shader, debugOut)
                    }
            }
        }
    }
}