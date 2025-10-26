package net.typho.vibrancy;

import net.minecraft.client.resources.model.BlockStateModelLoader;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

@Mod(Vibrancy.MOD_ID)
public class VibrancyForge {
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(BuiltInRegistries.PARTICLE_TYPE, Vibrancy.MOD_ID);
    public static final Supplier<SimpleParticleType> STEAM = PARTICLES.register(
            "steam",
            () -> new SimpleParticleType(false)
    );

    public VibrancyForge() {
        Vibrancy.BLOCK_STATE_PREDICATE = BlockStateModelLoader::predicate;
        Vibrancy.STEAM = STEAM;
        Vibrancy.init();
    }
}
