package net.typho.vibrancy

import com.mojang.serialization.Lifecycle
import net.fabricmc.api.ClientModInitializer
import net.minecraft.core.MappedRegistry
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.typho.vibrancy.block.BlockLightType
import net.typho.vibrancy.block.impl.RayPointLightType
import net.typho.vibrancy.block.impl.SubtleLightType

object VibrancyFabric : ClientModInitializer {
    @JvmField
    @Suppress("UNCHECKED_CAST")
    val blockLightRegistry: Registry<BlockLightType<*, *, *>> = Registry.register(
        BuiltInRegistries.REGISTRY as Registry<Registry<BlockLightType<*, *, *>>>,
        ResourceLocation.fromNamespaceAndPath(Vibrancy.MOD_ID, "block_light_types"), // TODO
        MappedRegistry(ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(Vibrancy.MOD_ID, "block_light_types")), Lifecycle.stable()) // TODO
    )

    override fun onInitializeClient() {
        Vibrancy.init()
        Registry.register(blockLightRegistry, ResourceLocation.fromNamespaceAndPath(Vibrancy.MOD_ID, "raytraced_point"), RayPointLightType)
        Registry.register(blockLightRegistry, ResourceLocation.fromNamespaceAndPath(Vibrancy.MOD_ID, "subtle"), SubtleLightType)
    }
}