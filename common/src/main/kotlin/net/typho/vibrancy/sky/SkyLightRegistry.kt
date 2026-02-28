package net.typho.vibrancy.sky

import com.mojang.serialization.MapCodec
import net.minecraft.core.Registry
import net.minecraft.world.level.Level
import net.typho.big_shot_lib.api.util.NeoRegistry
import net.typho.big_shot_lib.api.util.WrapperUtil
import net.typho.big_shot_lib.api.util.resources.NeoResourceKey
import net.typho.big_shot_lib.api.util.resources.ResourceIdentifier
import net.typho.vibrancy.Vibrancy

object SkyLightRegistry {
    @JvmField
    val registryKey: NeoResourceKey<Registry<SkyLightType<*, *>>> =
        NeoResourceKey.registry(Vibrancy.id("sky_light_types"))
    @JvmField
    var registry: NeoRegistry<SkyLightType<*, *>>? = null

    @JvmField
    val dimensionMap = HashMap<ResourceIdentifier, SkyLightInfo<*, *>>()

    @JvmStatic
    fun get(level: Level): SkyLightInfo<*, *>? = dimensionMap[WrapperUtil.INSTANCE.wrap(level.dimension()).location]

    @JvmStatic
    fun has(level: Level): Boolean = dimensionMap.containsKey(WrapperUtil.INSTANCE.wrap(level.dimension()).location)

    @JvmStatic
    fun infoCodec(level: ResourceIdentifier): MapCodec<SkyLightInfo<*, *>> {
        return NeoResourceKey.codec(registryKey).dispatchMap(
            { info -> registry!!.getKey(info.type()) },
            { key -> registry!!.get(key)!!.infoCodec(level) }
        )
    }
}