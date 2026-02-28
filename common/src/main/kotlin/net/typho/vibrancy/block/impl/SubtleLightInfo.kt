package net.typho.vibrancy.block.impl

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.util.ExtraCodecs
import net.minecraft.world.level.block.state.StateDefinition
import net.typho.vibrancy.util.StateFunction
import org.joml.Vector3f

@JvmRecord
data class SubtleLightInfo(
    @JvmField
    val color: StateFunction<Vector3f>,
    @JvmField
    val brightness: StateFunction<Float>,
    @JvmField
    val offset: StateFunction<Vector3f>,
    @JvmField
    val enabled: StateFunction<Boolean>
) {
    companion object {
        @JvmStatic
        fun codec(stateDefinition: StateDefinition<*, *>): MapCodec<SubtleLightInfo> = RecordCodecBuilder.mapCodec {
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
}