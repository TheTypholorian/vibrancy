package net.typho.vibrancy.sky

import net.minecraft.core.Registry
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.Level
import net.typho.big_shot_lib.api.event.NeoEventBus
import net.typho.big_shot_lib.api.event.NewRegistryEvent
import net.typho.big_shot_lib.api.event.RegisterEvent
import net.typho.big_shot_lib.api.event.RegistryBuilder
import net.typho.vibrancy.Vibrancy
import net.typho.vibrancy.sky.impl.OverworldSkyLightType

object SkyLightRegistry {
    @JvmField
    val registryKey: ResourceKey<Registry<SkyLightType<*, *>>> =
        ResourceKey.createRegistryKey(Vibrancy.id("sky_light_types"))
    lateinit var registry: Registry<SkyLightType<*, *>>

    @JvmField
    val dimensionMap = HashMap<ResourceKey<Level>, SkyLightInfo>()

    @JvmStatic
    fun get(level: Level): SkyLightInfo? = dimensionMap[level.dimension()]

    @JvmStatic
    fun has(level: Level): Boolean = dimensionMap.containsKey(level.dimension())

    @JvmStatic
    fun onInitialize(bus: NeoEventBus) {
        bus.register(NewRegistryEvent { output ->
            registry = output.register(RegistryBuilder(registryKey))
        })
        bus.register(RegisterEvent { output ->
            output.begin(registryKey) {
                it.register(Vibrancy.id("overworld"), OverworldSkyLightType)
            }
        })
    }
}