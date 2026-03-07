package net.typho.vibrancy.block.impl

import net.minecraft.world.level.block.state.StateDefinition
import net.typho.big_shot_lib.api.client.opengl.buffers.GlFramebuffer
import net.typho.big_shot_lib.api.client.opengl.shaders.NeoShaderRegistry
import net.typho.big_shot_lib.api.client.opengl.util.TextureUtil
import net.typho.big_shot_lib.api.client.util.events.RenderEventData
import net.typho.vibrancy.LightManager
import net.typho.vibrancy.LightRenderResult
import net.typho.vibrancy.Vibrancy
import net.typho.vibrancy.block.BlockLightType
import net.typho.vibrancy.block.HashMapBlockLightStorage
import net.typho.vibrancy.shadows.LightMesh

object RayPointLightType : BlockLightType<RayPointLightInfo, HashMapBlockLightStorage<RayPointLightInfo, RayPointLight>> {
    override fun infoCodec(stateDefinition: StateDefinition<*, *>) = RayPointLightInfo.codec(stateDefinition)

    override fun castInfo(info: Any?): RayPointLightInfo? {
        return info as? RayPointLightInfo
    }

    override fun createStorage(manager: LightManager) = RayPointLightStorage()

    override fun render(
        manager: LightManager,
        data: RenderEventData,
        lights: HashMapBlockLightStorage<RayPointLightInfo, RayPointLight>,
        fbo: GlFramebuffer
    ): LightRenderResult {
        val result = LightRenderResult()

        if (Vibrancy.config.blockLights.raytraced.enabled) {
            val settings = LightMesh.renderSettings(fbo, data, TextureUtil.INSTANCE.blockAtlas)
            val shader = NeoShaderRegistry.get(Vibrancy.id("light_mesh"))!! // TODO

            settings.bind()

            lights.map.values.forEach { it.update(manager, data) }

            lights.map.values
                .filter { light ->
                    data.frustum.testAab(
                        light.boundingBox.minPosition.toVector3f(),
                        light.boundingBox.maxPosition.toVector3f()
                    )
                }
                .sortedBy { light -> manager.getSortingOrder(data, light.blockPos) }
                .take(Vibrancy.config.blockLights.raytraced.maxRendered)
                .forEach { light -> result.add(light.render(shader)) }

            settings.unbind()
        }

        return result
    }
}