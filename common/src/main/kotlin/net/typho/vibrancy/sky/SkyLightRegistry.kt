package net.typho.vibrancy.sky

import net.minecraft.core.Registry
import net.minecraft.world.level.Level
import net.typho.big_shot_lib.api.util.NeoRegistry
import net.typho.big_shot_lib.api.util.RegistrationFactory
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
    val dimensionMap = HashMap<ResourceIdentifier, Any>()

    @JvmStatic
    fun <I> get(level: Level, type: SkyLightType<I, *>): I? = type.castInfo(dimensionMap[WrapperUtil.INSTANCE.wrap(level.dimension()).location])

    @JvmStatic
    fun has(level: Level): Boolean = dimensionMap.containsKey(WrapperUtil.INSTANCE.wrap(level.dimension()).location)

    @JvmStatic
    fun registerBuiltins(factory: RegistrationFactory) {
        val consumer = factory.begin(registryKey, Vibrancy.MOD_ID)
    }
}