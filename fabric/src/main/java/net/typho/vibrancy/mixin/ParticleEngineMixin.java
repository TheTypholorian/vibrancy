package net.typho.vibrancy.mixin;

import net.minecraft.client.particle.CampfireSmokeParticle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.typho.vibrancy.Vibrancy;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ParticleEngine.class)
public abstract class ParticleEngineMixin {
    @Shadow
    public abstract <T extends ParticleOptions> void register(ParticleType<T> p_107379_, ParticleEngine.SpriteParticleRegistration<T> p_107380_);

    @Inject(
            method = "registerProviders",
            at = @At("TAIL")
    )
    private void registerProviders(CallbackInfo ci) {
        register(Vibrancy.STEAM.get(), CampfireSmokeParticle.SignalProvider::new);
    }
}
