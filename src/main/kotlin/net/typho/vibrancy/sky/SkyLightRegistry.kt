package net.typho.vibrancy.sky

import net.minecraft.core.Registry
import net.minecraft.world.level.Level
import net.typho.big_shot_lib.api.util.NeoRegistry
import net.typho.big_shot_lib.api.util.RegistrationFactory
import net.typho.big_shot_lib.api.util.WrapperUtil
import net.typho.big_shot_lib.api.util.resource.NeoIdentifier
import net.typho.big_shot_lib.api.util.resource.NeoResourceKey
import net.typho.vibrancy.Vibrancy
import net.typho.vibrancy.sky.impl.OverworldSkyLightType

object SkyLightRegistry {
    @JvmField
    val registryKey: NeoResourceKey<Registry<SkyLightType<*, *>>> =
        NeoResourceKey.registry(Vibrancy.id("sky_light_types"))
    @JvmField
    var registry: NeoRegistry<SkyLightType<*, *>>? = null

    @JvmField
    val dimensionMap = HashMap<NeoIdentifier, SkyLightInfo>()

    @JvmStatic
    fun get(level: Level): SkyLightInfo? = dimensionMap[WrapperUtil.INSTANCE.wrap(level.dimension()).location]

    @JvmStatic
    fun has(level: Level): Boolean = dimensionMap.containsKey(WrapperUtil.INSTANCE.wrap(level.dimension()).location)

    @JvmStatic
    fun registerBuiltins(factory: RegistrationFactory) {
        factory.begin(registryKey, Vibrancy.MOD_ID)?.run {
            register("overworld") { OverworldSkyLightType }
        }
    }
}