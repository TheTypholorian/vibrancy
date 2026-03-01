package net.typho.vibrancy.sky.impl

import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.typho.big_shot_lib.api.util.resources.NeoCodecs
import net.typho.vibrancy.sky.SkyLightInfo
import net.typho.vibrancy.sky.SkyLightType
import org.joml.Vector3fc

@JvmRecord
data class OverworldSkyLightInfo(
    @JvmField
    val sunColor: Vector3fc,
    @JvmField
    val moonColor: Vector3fc
) : SkyLightInfo {
    override val type: SkyLightType<*, *>
        get() = OverworldSkyLightType

    companion object {
        @JvmField
        val CODEC: MapCodec<OverworldSkyLightInfo> = RecordCodecBuilder.mapCodec {
            it.group(
                NeoCodecs.VEC3F
                    .fieldOf("sunColor")
                    .forGetter { info -> info.sunColor },
                NeoCodecs.VEC3F
                    .fieldOf("moonColor")
                    .forGetter { info -> info.moonColor },
            ).apply(it, ::OverworldSkyLightInfo)
        }
    }
}