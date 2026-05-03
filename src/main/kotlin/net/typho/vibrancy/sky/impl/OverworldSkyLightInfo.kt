package net.typho.vibrancy.sky.impl

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.typho.vibrancy.sky.SkyLightInfo

data class OverworldSkyLightInfo(
    @JvmField
    val brightness: Float
) : SkyLightInfo {
    override val type = OverworldSkyLightType

    companion object {
        @JvmField
        val CODEC: MapCodec<OverworldSkyLightInfo> = RecordCodecBuilder.mapCodec {
            it.group(
                Codec.FLOAT
                    .fieldOf("brightness")
                    .forGetter { info -> info.brightness }
            ).apply(it, ::OverworldSkyLightInfo)
        }
    }
}
