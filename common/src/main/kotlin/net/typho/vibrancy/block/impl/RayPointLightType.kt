package net.typho.vibrancy.block.impl

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.util.ExtraCodecs
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
import net.typho.vibrancy.util.StateFunction
import org.joml.Vector3f

object RayPointLightType : BlockLightType<RayPointLightInfo, RayPointLight, HashMapBlockLightStorage<RayPointLightInfo, RayPointLight>> {
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
                    BlendFactor.ONE,
                    BlendFactor.ONE
                )
            )
        )
    )

    override fun infoCodec(stateDefinition: StateDefinition<*, *>): MapCodec<RayPointLightInfo> {
        return RecordCodecBuilder.mapCodec {
            it.group(
                StateFunction.codec(ExtraCodecs.VECTOR3F, stateDefinition)
                    .fieldOf("color")
                    .forGetter { info -> info.color },
                StateFunction.codec(Codec.FLOAT, stateDefinition)
                    .fieldOf("radius")
                    .forGetter { info -> info.radius },
                StateFunction.codec(Codec.FLOAT, stateDefinition)
                    .fieldOf("brightness")
                    .forGetter { info -> info.brightness },
                StateFunction.codec(ExtraCodecs.VECTOR3F, stateDefinition)
                    .optionalFieldOf("offset", StateFunction(Vector3f(0.5f)))
                    .forGetter { info -> info.offset },
                StateFunction.codec(Codec.BOOL, stateDefinition)
                    .optionalFieldOf("enabled", StateFunction(true))
                    .forGetter { info -> info.enabled }
            ).apply(it, ::RayPointLightInfo)
        }
    }

    override fun createStorage(manager: LightManager) = HashMapBlockLightStorage(this)

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
                    manager.inRenderDistance(data, light.pos, Vibrancy.config.blockLights.raytraced.renderDistance)
                            && data.frustum.testAab(light.getBoundingBox().minPosition.toVector3f(), light.getBoundingBox().maxPosition.toVector3f())
                }
                .sorted(Comparator.comparingDouble { light -> manager.getSortingOrder(data, light.pos).toDouble() })
                .limit(Vibrancy.config.blockLights.raytraced.maxRendered.toLong())
                .forEachOrdered { light ->
                    val raytrace = (result.numRaytraced ?: 0) < Vibrancy.config.blockLights.raytraced.maxRaytraced
                            && manager.inRenderDistance(data, light.pos, Vibrancy.config.blockLights.raytraced.raytraceDistance)
                    result.add(
                        light.render(
                            manager,
                            data,
                            raytrace,
                            raytrace
                                    && (result.numForeground ?: 0) < Vibrancy.config.blockLights.raytraced.maxForeground
                                    && manager.inRenderDistance(data, light.pos, Vibrancy.config.blockLights.raytraced.foregroundDistance),
                            fbo
                        )
                    )
                }

            renderSettings.unbind()
        }

        return result
    }
}