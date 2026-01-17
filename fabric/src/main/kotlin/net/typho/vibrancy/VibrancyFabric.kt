package net.typho.vibrancy

import com.mojang.serialization.Lifecycle
import net.fabricmc.api.ClientModInitializer
import net.minecraft.core.MappedRegistry
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.typho.vibrancy.block.BlockLightRegistry
import net.typho.vibrancy.block.BlockLightType
import net.typho.vibrancy.block.impl.RayPointLightType
import net.typho.vibrancy.block.impl.SubtleLightType

object VibrancyFabric : ClientModInitializer {
    @JvmField
    @Suppress("UNCHECKED_CAST")
    val blockLightRegistry: Registry<BlockLightType<*, *, *>> = Registry.register(
        BuiltInRegistries.REGISTRY as Registry<Registry<BlockLightType<*, *, *>>>,
        BlockLightRegistry.registryKey,
        MappedRegistry(BlockLightRegistry.registryKey, Lifecycle.stable())
    )

    override fun onInitializeClient() {
        Vibrancy.init()
        Registry.register(blockLightRegistry, Vibrancy.id("raytraced_point"), RayPointLightType)
        Registry.register(blockLightRegistry, Vibrancy.id("subtle"), SubtleLightType)
    }
}