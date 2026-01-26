package net.typho.vibrancy.block.impl

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.RenderType
import net.minecraft.util.ExtraCodecs
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.phys.AABB
import net.typho.big_shot_lib.api.IFramebuffer
import net.typho.big_shot_lib.gl.GlStack
import net.typho.big_shot_lib.gl.state.BlendFactor
import net.typho.big_shot_lib.gl.state.BlendFunction
import net.typho.big_shot_lib.gl.state.CullFace
import net.typho.big_shot_lib.gl.state.GlCapability
import net.typho.vibrancy.LightManager
import net.typho.vibrancy.Vibrancy
import net.typho.vibrancy.block.BlockLightType
import net.typho.vibrancy.block.BlockRenderResult
import net.typho.vibrancy.block.HashMapBlockLightStorage
import net.typho.vibrancy.util.StateFunction
import org.joml.Vector3f

object RayPointLightType : BlockLightType<RayPointLightInfo, RayPointLight, HashMapBlockLightStorage<RayPointLightInfo, RayPointLight>> {
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
        lights: HashMapBlockLightStorage<RayPointLightInfo, RayPointLight>,
        fbo: IFramebuffer
    ): BlockRenderResult {
        val result = BlockRenderResult(
            numRendered = 0,
            numRaytraced = 0,
            numShadows = 0,
            numAsyncTasks = 0,
            numEntityShadowCalls = 0
        )

        if (Vibrancy.config.blockLights.raytraced.enabled) {
            GlStack().use { stack ->
                stack.disable(GlCapability.DEPTH_TEST)
                //stack.enable(GlCapability.STENCIL_TEST)
                stack.enable(GlCapability.CULL_FACE)
                stack.set(CullFace.FRONT)
                stack.enable(GlCapability.BLEND)
                stack.set(
                    BlendFunction(
                        BlendFactor.ONE,
                        BlendFactor.ONE
                    )
                )
                /*
                stack.set(StencilMask, LightManager.SHADOW_STENCIL_MASK)
                stack.set(
                    StencilFunc(
                        ComparisonMode.NOTEQUAL,
                        LightManager.SHADOW_STENCIL_MASK,
                        LightManager.SHADOW_STENCIL_MASK
                    )
                )
                 */

                lights.map.values.stream()
                    .filter { light ->
                        manager.inRenderDistance(light.pos, Vibrancy.config.blockLights.raytraced.renderDistance.get())
                                && manager.inFrustum(light.getBoundingBox())
                    }
                    .sorted(Comparator.comparingDouble { light -> manager.getSortingOrder(light.pos) })
                    .limit(Vibrancy.config.blockLights.raytraced.maxRendered.get().toLong())
                    .forEachOrdered { light ->
                        result.add(
                            light.render(
                                manager,
                                result.numRaytraced!! < Vibrancy.config.blockLights.raytraced.maxRaytraced.get()
                                        && manager.inRenderDistance(light.pos, Vibrancy.config.blockLights.raytraced.raytraceDistance.get()),
                                stack,
                                fbo
                            )
                        )
                    }
            }
        }

        return result
    }

    override fun renderDebug(
        manager: LightManager,
        lights: HashMapBlockLightStorage<RayPointLightInfo, RayPointLight>
    ) {
        /*
        lights.map.values.stream()
            .sorted(Comparator.comparingDouble { light -> manager.getSortingOrder(light.pos) })
            .limit(10)
            .forEachOrdered { light ->
                light.shadows.renderDebug(
                    manager,
                    Minecraft.getInstance().renderBuffers().bufferSource().getBuffer(RenderType.lines()),
                    light.getAbsolutePos(),
                    light.radius
                )
            }
         */
    }

    override fun getEntityShadowBoxes(
        manager: LightManager,
        lights: HashMapBlockLightStorage<RayPointLightInfo, RayPointLight>
    ): Iterable<AABB>? {
        return if (Vibrancy.config.blockLights.raytraced.entityShadows) {
            lights.map.values.map { light -> light.getShadowBox().aabb() }
        } else {
            null
        }
    }
}