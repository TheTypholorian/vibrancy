package net.typho.vibrancy.sky

import com.mojang.serialization.Lifecycle
import com.mojang.serialization.MapCodec
import net.minecraft.core.MappedRegistry
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.block.Block
import net.typho.vibrancy.Vibrancy
import java.util.logging.Level

object SkyLightRegistry {
    @JvmField
    val typesKey: ResourceKey<Registry<SkyLightType<*, *, *>>> =
        ResourceKey.createRegistryKey(Vibrancy.id("sky_light_types"))
    @JvmField
    @Suppress("UNCHECKED_CAST")
    val types: Registry<SkyLightType<*, *, *>> = Registry.register(
        BuiltInRegistries.REGISTRY as Registry<Registry<SkyLightType<*, *, *>>>,
        typesKey,
        MappedRegistry(typesKey, Lifecycle.stable())
    )

    @JvmField
    val dimensionMap = HashMap<ResourceKey<Level>, SkyLightInfo<*, *>>()

    @JvmStatic
    fun get(level: ResourceKey<Level>): SkyLightInfo<*, *>? = dimensionMap[level]

    @JvmStatic
    fun has(level: ResourceKey<Level>): Boolean = dimensionMap.containsKey(level)

    @JvmStatic
    fun infoCodec(level: ResourceKey<Level>): MapCodec<SkyLightInfo<*, *>> {
        return ResourceKey.codec(typesKey).dispatchMap(
            { info -> types.getResourceKey(info.type()).orElseThrow() },
            { key -> types.get(key)!!.infoCodec(level) }
        )
    }

    fun init() = Unit
}