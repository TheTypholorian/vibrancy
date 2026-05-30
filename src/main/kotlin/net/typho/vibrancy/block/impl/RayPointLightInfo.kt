package net.typho.vibrancy.block.impl

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.world.level.block.state.StateDefinition
import net.typho.big_shot_lib.api.math.vec.IVec3
import net.typho.big_shot_lib.api.math.vec.NeoVec3f
import net.typho.vibrancy.block.BlockLightInfo
import net.typho.vibrancy.util.StateFunction

data class RayPointLightInfo(
    @JvmField
    val color: StateFunction<IVec3<Float>>,
    @JvmField
    val flicker: StateFunction<Float>,
    @JvmField
    val radius: StateFunction<Float>,
    @JvmField
    val brightness: StateFunction<Float>,
    @JvmField
    val offset: StateFunction<IVec3<Float>>,
    override val enabled: StateFunction<Boolean>
) : BlockLightInfo {
    override val type = RayPointLightType

    companion object {
        @JvmStatic
        fun codec(stateDefinition: StateDefinition<*, *>): MapCodec<RayPointLightInfo> = RecordCodecBuilder.mapCodec {
            it.group(
                StateFunction.codec(IVec3.FLOAT_CODEC, stateDefinition)
                    .fieldOf("color")
                    .forGetter { info -> info.color },
                StateFunction.codec(Codec.FLOAT, stateDefinition)
                    .optionalFieldOf("flicker", StateFunction(0f))
                    .forGetter { info -> info.flicker },
                StateFunction.codec(Codec.FLOAT, stateDefinition)
                    .fieldOf("radius")
                    .forGetter { info -> info.radius },
                StateFunction.codec(Codec.FLOAT, stateDefinition)
                    .fieldOf("brightness")
                    .forGetter { info -> info.brightness },
                StateFunction.codec(IVec3.FLOAT_CODEC, stateDefinition)
                    .optionalFieldOf("offset", StateFunction(NeoVec3f(0.5f, 0.5f, 0.5f)))
                    .forGetter { info -> info.offset },
                StateFunction.codec(Codec.BOOL, stateDefinition)
                    .optionalFieldOf("enabled", StateFunction(true))
                    .forGetter { info -> info.enabled }
            ).apply(it, ::RayPointLightInfo)
        }
    }
}