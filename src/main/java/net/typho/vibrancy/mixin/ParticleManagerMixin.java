package net.typho.vibrancy.mixin;

import net.minecraft.client.particle.ParticleManager;
import net.typho.vibrancy.Vibrancy;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ParticleManager.class)
public class ParticleManagerMixin {
    @Inject(
            method = "registerDefaultFactories",
            at = @At("TAIL")
    )
    private void registerDefaultFactories(CallbackInfo ci) {
        Vibrancy.registerParticles((ParticleManager) (Object) this);
    }
}
