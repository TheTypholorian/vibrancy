package net.typho.vibrancy.block.impl

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.util.ExtraCodecs
import net.minecraft.world.level.block.state.StateDefinition
import net.typho.big_shot_lib.api.client.rendering.event.RenderData
import net.typho.big_shot_lib.api.client.rendering.state.*
import net.typho.big_shot_lib.api.client.rendering.textures.GlFramebuffer
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
        data: RenderData,
        lights: HashMapBlockLightStorage<RayPointLightInfo, RayPointLight>,
        fbo: GlFramebuffer
    ): LightRenderResult {
        val result = LightRenderResult(
            numRendered = 0,
            numRaytraced = 0,
            numShadows = 0,
            numAsyncTasks = 0
        )

        if (Vibrancy.config.blockLights.raytraced.enabled) {
            renderSettings.bind()

            lights.map.values.stream()
                .filter { light ->
                    manager.inRenderDistance(light.pos, Vibrancy.config.blockLights.raytraced.renderDistance.get())
                            && data.frustum.testAab(light.getBoundingBox().minPosition.toVector3f(), light.getBoundingBox().maxPosition.toVector3f())
                }
                .sorted(Comparator.comparingDouble { light -> manager.getSortingOrder(light.pos) })
                .limit(Vibrancy.config.blockLights.raytraced.maxRendered.get().toLong())
                .forEachOrdered { light ->
                    result.add(
                        light.render(
                            manager,
                            data,
                            result.numRaytraced!! < Vibrancy.config.blockLights.raytraced.maxRaytraced.get()
                                    && manager.inRenderDistance(light.pos, Vibrancy.config.blockLights.raytraced.raytraceDistance.get()),
                            fbo
                        )
                    )
                }

            renderSettings.unbind()
        }

        return result
    }
}