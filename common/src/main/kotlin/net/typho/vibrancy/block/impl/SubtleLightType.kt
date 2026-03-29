package net.typho.vibrancy.block.impl

import net.minecraft.world.level.block.state.StateDefinition
import net.typho.big_shot_lib.api.client.rendering.opengl.state.GlDrawState
import net.typho.big_shot_lib.api.client.rendering.opengl.state.GlShaderShard
import net.typho.big_shot_lib.api.client.rendering.quad.NeoAtlas
import net.typho.big_shot_lib.api.client.util.event.RenderEventData
import net.typho.vibrancy.LightManager
import net.typho.vibrancy.Vibrancy
import net.typho.vibrancy.block.BlockLightType
import net.typho.vibrancy.shadows.LightMesh

object SubtleLightType : BlockLightType<SubtleLightInfo, SubtleLightStorage> {
    @JvmField
    val meshBlitDrawState = GlDrawState.Basic(
        shader = GlShaderShard.FromLocation(
            Vibrancy.id("block/subtle/blit"),
            {
                setUniform("LightBrightness") { set(Vibrancy.config.blockLights.subtle.brightness) }
            }
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
        if (Vibrancy.config.blockLights.subtle.enabled) {
            lights.checkDirty(manager, data)

            LightMesh.drawState(NeoAtlas.blocks, Vibrancy.id("block/subtle/mesh")).bind().use { settings ->
                lights.chunks.values
                    .filter {
                        manager.inRenderDistance(data, it.pos, Vibrancy.config.blockLights.subtle.renderDistance)
                    }
                    .forEach {
                        it.render(data, settings.shader, debugOut)
                    }
            }
        }
    }
}