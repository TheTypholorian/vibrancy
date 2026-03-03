package net.typho.vibrancy.block.impl

import net.minecraft.world.level.block.state.StateDefinition
import net.typho.big_shot_lib.api.client.opengl.buffers.GlFramebuffer
import net.typho.big_shot_lib.api.client.util.events.RenderEventData
import net.typho.vibrancy.LightManager
import net.typho.vibrancy.LightRenderResult
import net.typho.vibrancy.Vibrancy
import net.typho.vibrancy.block.BlockLightType
import net.typho.vibrancy.block.HashMapBlockLightStorage

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
            lights.map.values.stream()
                .filter { light ->
                    data.frustum.testAab(light.boundingBox.minPosition.toVector3f(), light.boundingBox.maxPosition.toVector3f())
                }
                .sorted(Comparator.comparingDouble { light -> manager.getSortingOrder(data, light.blockPos).toDouble() })
                .limit(Vibrancy.config.blockLights.raytraced.maxRendered.toLong())
                .forEachOrdered { light ->
                    result.add(
                        light.render(
                            manager,
                            data,
                            fbo
                        )
                    )
                }
        }

        return result
    }
}