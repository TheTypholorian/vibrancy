package net.typho.vibrancy;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

@Mod(Vibrancy.MOD_ID)
public class VibrancyNeoForge {
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(BuiltInRegistries.PARTICLE_TYPE, Vibrancy.MOD_ID);
    public static final Supplier<SimpleParticleType> STEAM = PARTICLES.register(
            "steam",
            () -> new SimpleParticleType(false)
    );

    public VibrancyNeoForge() {
        Vibrancy.STEAM = STEAM;
        Vibrancy.init();
    }
}
