package net.typho.vibrancy.sky.impl

import net.typho.big_shot_lib.api.client.opengl.buffers.GlFramebuffer
import net.typho.big_shot_lib.api.client.opengl.shaders.NeoShaderRegistry
import net.typho.big_shot_lib.api.client.opengl.util.TextureUtil
import net.typho.big_shot_lib.api.client.util.events.RenderEventData
import net.typho.vibrancy.LightManager
import net.typho.vibrancy.Vibrancy
import net.typho.vibrancy.shadows.LightMesh
import net.typho.vibrancy.sky.SkyLightInfo
import net.typho.vibrancy.sky.SkyLightType

object OverworldSkyLightType : SkyLightType<OverworldSkyLightInfo, OverworldSkyLightStorage> {
    override val infoCodec = OverworldSkyLightInfo.CODEC

    override fun createStorage(manager: LightManager) = OverworldSkyLightStorage()

    override fun castInfo(info: SkyLightInfo?): OverworldSkyLightInfo? {
        return info as? OverworldSkyLightInfo
    }

    var lastBlit: Long = 0

    override fun render(
        manager: LightManager,
        data: RenderEventData,
        lights: OverworldSkyLightStorage,
        fbo: GlFramebuffer,
        debugOut: (String, Int) -> Unit
    ) {
        val time = System.currentTimeMillis()

        if (time - lastBlit >= 100) { // TODO
            lastBlit = time
            val blitSettings = OverworldSkyLightStorage.meshBlitSettings(data, lights)
            blitSettings.bind()
            lights.chunks.values.forEach { it.updateShadows(manager, data, debugOut) }
            blitSettings.unbind()
        }

        val settings = LightMesh.renderSettings(fbo, data, TextureUtil.INSTANCE.blockAtlas)
        val shader = NeoShaderRegistry.get(Vibrancy.id("light_mesh"))!! // TODO
        settings.bind()
        val result = lights.chunks.values.forEach { it.render(shader, debugOut) }
        settings.unbind()
        return result
    }
}