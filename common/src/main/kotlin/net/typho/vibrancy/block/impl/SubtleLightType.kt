package net.typho.vibrancy.block.impl

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.util.ExtraCodecs
import net.minecraft.world.level.block.state.StateDefinition
import net.typho.big_shot_lib.gl.GlStack
import net.typho.big_shot_lib.gl.state.BlendFactor
import net.typho.big_shot_lib.gl.state.BlendFunction
import net.typho.big_shot_lib.gl.state.CullFace
import net.typho.big_shot_lib.gl.state.GlCapability
import net.typho.vibrancy.LightManager
import net.typho.vibrancy.block.BlockLightType
import net.typho.vibrancy.block.RenderingBlockLight
import net.typho.vibrancy.util.StateFunction
import org.joml.Vector3f

object SubtleLightType : BlockLightType<SubtleLightInfo, SubtleLight> {
    override fun codec(stateDefinition: StateDefinition<*, *>): MapCodec<SubtleLightInfo> {
        return RecordCodecBuilder.mapCodec {
            it.group(
                StateFunction.codec(ExtraCodecs.VECTOR3F, stateDefinition)
                    .fieldOf("color")
                    .forGetter { info -> info.color },
                StateFunction.codec(Codec.FLOAT, stateDefinition)
                    .fieldOf("brightness")
                    .forGetter { info -> info.brightness },
                StateFunction.codec(ExtraCodecs.VECTOR3F, stateDefinition)
                    .optionalFieldOf("offset", StateFunction(Vector3f(0.5f)))
                    .forGetter { info -> info.offset },
                StateFunction.codec(Codec.BOOL, stateDefinition)
                    .optionalFieldOf("enabled", StateFunction(true))
                    .forGetter { info -> info.enabled }
            ).apply(it, ::SubtleLightInfo)
        }
    }

    override fun render(manager: LightManager, lights: Set<RenderingBlockLight<SubtleLight>>) {
        GlStack().use { stack ->
            stack.disable(GlCapability.DEPTH_TEST)
            stack.disable(GlCapability.STENCIL_TEST)
            stack.enable(GlCapability.CULL_FACE)
            stack.set(CullFace.FRONT)
            stack.enable(GlCapability.BLEND)
            stack.set(
                BlendFunction(
                    BlendFactor.ONE,
                    BlendFactor.ONE
                )
            )

            lights.stream()
                .sorted(Comparator.comparingDouble { manager.getSortingOrder(it.light) })
                .forEachOrdered { light -> light.light.render(manager, light, stack) }
        }
    }
}