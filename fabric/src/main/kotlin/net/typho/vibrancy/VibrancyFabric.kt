package net.typho.vibrancy

import com.mojang.serialization.Lifecycle
import net.fabricmc.api.ClientModInitializer
import net.minecraft.core.MappedRegistry
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.typho.vibrancy.block.BlockLightRegistry
import net.typho.vibrancy.block.BlockLightType

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
    }
}