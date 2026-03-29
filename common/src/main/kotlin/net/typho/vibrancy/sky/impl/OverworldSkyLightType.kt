package net.typho.vibrancy.sky.impl

import net.typho.big_shot_lib.api.client.util.event.RenderEventData
import net.typho.vibrancy.LightManager
import net.typho.vibrancy.sky.SkyLightInfo
import net.typho.vibrancy.sky.SkyLightType

object OverworldSkyLightType : SkyLightType<OverworldSkyLightInfo, OverworldSkyLightStorage> {
    override val infoCodec = OverworldSkyLightInfo.CODEC

    override fun createStorage(manager: LightManager) = OverworldSkyLightStorage()

    override fun castInfo(info: SkyLightInfo?): OverworldSkyLightInfo? {
        return info as? OverworldSkyLightInfo
    }

    override fun render(
        manager: LightManager,
        data: RenderEventData,
        lights: OverworldSkyLightStorage,
        debugOut: (String, Int) -> Unit
    ) {
        OverworldSkyLightStorage.meshBlitDrawState.bind().use { state ->
            state.shader.setUniform("LightDirection") { set(lights.getLightDirection(data.level)) }
            lights.chunks.values.forEach { it.updateShadows(manager, data, debugOut) }
        }

        // TODO
        /*
        val settings = LightMesh.renderSettings(fbo, data, NeoAtlas.blocks)
        val shader = NeoShaderRegistry.get(Vibrancy.id("mesh"))!! // TODO
        settings.bind()
        lights.chunks.values.forEach { it.render(shader, debugOut) }
        settings.unbind()
         */
    }
}