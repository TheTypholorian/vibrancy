package net.typho.vibrancy

import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.neoforged.bus.api.IEventBus
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.ModContainer
import net.neoforged.fml.common.Mod
import net.neoforged.neoforge.registries.NewRegistryEvent
import net.neoforged.neoforge.registries.RegisterEvent
import net.neoforged.neoforge.registries.RegistryBuilder
import net.typho.vibrancy.block.BlockLightType
import net.typho.vibrancy.block.impl.RayPointLightType
import net.typho.vibrancy.block.impl.SubtleLightType

@Mod(Vibrancy.MOD_ID)
class VibrancyNeoForge(eventBus: IEventBus, mod: ModContainer) {
    init {
        Vibrancy.init()
        eventBus.register(this)
    }

    @SubscribeEvent
    fun onNewRegistry(event: NewRegistryEvent) {
        event.register(
            RegistryBuilder(ResourceKey.createRegistryKey<BlockLightType<*, *, *>>(ResourceLocation.fromNamespaceAndPath(Vibrancy.MOD_ID, "block_light_types"))).create()
        )
    }

    @SubscribeEvent
    fun onRegister(event: RegisterEvent) {
        event.register(ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(Vibrancy.MOD_ID, "block_light_types"))) { registrar ->
            registrar.register(ResourceLocation.fromNamespaceAndPath(Vibrancy.MOD_ID, "raytraced_point"), RayPointLightType)
            registrar.register(ResourceLocation.fromNamespaceAndPath(Vibrancy.MOD_ID, "subtle"), SubtleLightType)
        }
    }
}