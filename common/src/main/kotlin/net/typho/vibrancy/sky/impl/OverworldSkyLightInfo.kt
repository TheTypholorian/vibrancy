package net.typho.vibrancy.sky.impl

import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.typho.big_shot_lib.api.math.vec.IVec3
import net.typho.vibrancy.sky.SkyLightInfo

data class OverworldSkyLightInfo(
    @JvmField
    val sunColor: IVec3<Float>,
    @JvmField
    val moonColor: IVec3<Float>
) : SkyLightInfo {
    override val type = OverworldSkyLightType

    companion object {
        @JvmField
        val codec: MapCodec<OverworldSkyLightInfo> = RecordCodecBuilder.mapCodec {
            it.group(
                IVec3.FLOAT_CODEC
                    .fieldOf("sunColor")
                    .forGetter { info -> info.sunColor },
                IVec3.FLOAT_CODEC
                    .fieldOf("moonColor")
                    .forGetter { info -> info.moonColor },
            ).apply(it, ::OverworldSkyLightInfo)
        }
    }
}