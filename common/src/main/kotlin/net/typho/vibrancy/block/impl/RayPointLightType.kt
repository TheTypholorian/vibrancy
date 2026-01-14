package net.typho.vibrancy.block.impl

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.util.ExtraCodecs
import net.minecraft.world.level.block.state.StateDefinition
import net.typho.big_shot_lib.gl.GlStack
import net.typho.big_shot_lib.gl.state.*
import net.typho.vibrancy.LightManager
import net.typho.vibrancy.block.BlockLightType
import net.typho.vibrancy.util.StateFunction
import org.joml.Vector3f

object RayPointLightType : BlockLightType<RayPointLightInfo, RayPointLight, RayPointLightStorage> {
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

    override fun createStorage() = RayPointLightStorage()

    override fun render(
        manager: LightManager,
        lights: RayPointLightStorage
    ): RenderResult {
        val result = RenderResult()

        GlStack().use { stack ->
            stack.disable(GlCapability.DEPTH_TEST)
            stack.enable(GlCapability.STENCIL_TEST)
            stack.enable(GlCapability.CULL_FACE)
            stack.set(CullFace.FRONT)
            stack.enable(GlCapability.BLEND)
            stack.set(
                BlendFunction(
                    BlendFactor.ONE,
                    BlendFactor.ONE
                )
            )
            stack.set(StencilMask, LightManager.SHADOW_STENCIL_MASK)
            stack.set(
                StencilFunc(
                    ComparisonMode.NOTEQUAL,
                    LightManager.SHADOW_STENCIL_MASK,
                    LightManager.SHADOW_STENCIL_MASK
                )
            )

            // TODO render
        }

        return result
    }
}