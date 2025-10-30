package net.typho.vibrancy;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;

public class VibrancyFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        SimpleParticleType type = Registry.register(BuiltInRegistries.PARTICLE_TYPE, Vibrancy.id("steam"), new SimpleParticleType(false) {});
        Vibrancy.STEAM = () -> type;
        Vibrancy.init();
    }
}
