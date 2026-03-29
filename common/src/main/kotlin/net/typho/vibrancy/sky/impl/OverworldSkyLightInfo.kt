package net.typho.vibrancy.sky.impl

import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.typho.big_shot_lib.api.math.vec.AbstractVec3
import net.typho.vibrancy.sky.SkyLightInfo
import net.typho.vibrancy.sky.SkyLightType

@JvmRecord
data class OverworldSkyLightInfo(
    @JvmField
    val sunColor: AbstractVec3<Float>,
    @JvmField
    val moonColor: AbstractVec3<Float>
) : SkyLightInfo {
    override val type: SkyLightType<*, *>
        get() = OverworldSkyLightType

    companion object {
        @JvmField
        val CODEC: MapCodec<OverworldSkyLightInfo> = RecordCodecBuilder.mapCodec {
            it.group(
                AbstractVec3.FLOAT_CODEC
                    .fieldOf("sunColor")
                    .forGetter { info -> info.sunColor },
                AbstractVec3.FLOAT_CODEC
                    .fieldOf("moonColor")
                    .forGetter { info -> info.moonColor },
            ).apply(it, ::OverworldSkyLightInfo)
        }
    }
}