package net.typho.vibrancy.sky

import com.mojang.serialization.MapCodec
import net.minecraft.core.Registry
import net.minecraft.core.RegistryAccess
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.Level
import net.typho.vibrancy.Vibrancy

object SkyLightRegistry {
    @JvmField
    val registryKey: ResourceKey<Registry<SkyLightType<*, *>>> =
        ResourceKey.createRegistryKey(Vibrancy.id("sky_light_types"))

    @JvmField
    val dimensionMap = HashMap<Level, SkyLightInfo<*, *>>()

    @JvmStatic
    fun get(level: Level): SkyLightInfo<*, *>? = dimensionMap[level]

    @JvmStatic
    fun has(level: Level): Boolean = dimensionMap.containsKey(level)

    @JvmStatic
    fun infoCodec(level: Level, registryAccess: RegistryAccess): MapCodec<SkyLightInfo<*, *>> {
        val types = registryAccess.registryOrThrow(registryKey)
        return ResourceKey.codec(registryKey).dispatchMap(
            { info -> types.getResourceKey(info.type()).orElseThrow() },
            { key -> types.get(key)!!.infoCodec(level) }
        )
    }

    fun init() = Unit
}