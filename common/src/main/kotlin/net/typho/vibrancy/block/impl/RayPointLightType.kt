package net.typho.vibrancy.block.impl

import net.minecraft.world.level.block.state.StateDefinition
import net.typho.big_shot_lib.api.client.opengl.buffers.GlFramebuffer
import net.typho.big_shot_lib.api.client.opengl.state.*
import net.typho.big_shot_lib.api.client.util.events.RenderEventData
import net.typho.big_shot_lib.api.util.IColor
import net.typho.vibrancy.LightManager
import net.typho.vibrancy.LightRenderResult
import net.typho.vibrancy.Vibrancy
import net.typho.vibrancy.block.BlockLightType
import net.typho.vibrancy.block.HashMapBlockLightStorage

object RayPointLightType : BlockLightType<RayPointLightInfo, HashMapBlockLightStorage<RayPointLightInfo, RayPointLight>> {
    @JvmField
    val renderSettings = RenderSettings(
        Vibrancy.id("ray_point_light"),
        listOf(
            CullShard(true, CullFace.FRONT),
            BlendShard(
                true,
                IColor.FULL_ON,
                BlendEquation.ADD,
                BlendFunction.Basic(
                    BlendFactor.SRC_ALPHA,
                    BlendFactor.ONE
                )
            )
        )
    )

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
            renderSettings.bind()

            lights.map.values.stream()
                .filter { light ->
                    manager.inRenderDistance(data, light.blockPos, Vibrancy.config.blockLights.raytraced.renderDistance)
                            && data.frustum.testAab(light.boundingBox.minPosition.toVector3f(), light.boundingBox.maxPosition.toVector3f())
                }
                .sorted(Comparator.comparingDouble { light -> manager.getSortingOrder(data, light.blockPos).toDouble() })
                .limit(Vibrancy.config.blockLights.raytraced.maxRendered.toLong())
                .forEachOrdered { light ->
                    val raytrace = (result.numRaytraced ?: 0) < Vibrancy.config.blockLights.raytraced.maxRaytraced
                            && manager.inRenderDistance(data, light.blockPos, Vibrancy.config.blockLights.raytraced.raytraceDistance)
                    result.add(
                        light.render(
                            manager,
                            data,
                            raytrace,
                            fbo
                        )
                    )
                }

            renderSettings.unbind()
        }

        return result
    }
}