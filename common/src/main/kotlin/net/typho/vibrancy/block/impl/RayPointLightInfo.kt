package net.typho.vibrancy.block.impl

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.world.level.block.state.StateDefinition
import net.typho.big_shot_lib.api.util.resources.NeoCodecs
import net.typho.vibrancy.util.StateFunction
import org.joml.Vector3f
import org.joml.Vector3fc

@JvmRecord
data class RayPointLightInfo(
    @JvmField
    val color: StateFunction<Vector3fc>,
    @JvmField
    val radius: StateFunction<Float>,
    @JvmField
    val brightness: StateFunction<Float>,
    @JvmField
    val offset: StateFunction<Vector3fc>,
    @JvmField
    val enabled: StateFunction<Boolean>
) {
    companion object {
        @JvmStatic
        fun codec(stateDefinition: StateDefinition<*, *>): MapCodec<RayPointLightInfo> = RecordCodecBuilder.mapCodec {
            it.group(
                StateFunction.codec(NeoCodecs.VEC3F, stateDefinition)
                    .fieldOf("color")
                    .forGetter { info -> info.color },
                StateFunction.codec(Codec.FLOAT, stateDefinition)
                    .fieldOf("radius")
                    .forGetter { info -> info.radius },
                StateFunction.codec(Codec.FLOAT, stateDefinition)
                    .fieldOf("brightness")
                    .forGetter { info -> info.brightness },
                StateFunction.codec(NeoCodecs.VEC3F, stateDefinition)
                    .optionalFieldOf("offset", StateFunction(Vector3f(0.5f)))
                    .forGetter { info -> info.offset },
                StateFunction.codec(Codec.BOOL, stateDefinition)
                    .optionalFieldOf("enabled", StateFunction(true))
                    .forGetter { info -> info.enabled }
            ).apply(it, ::RayPointLightInfo)
        }
    }
}