package net.typho.vibrancy.mixin;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.typho.vibrancy.RaytracedPointEntityLight;
import net.typho.vibrancy.Vibrancy;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerMixin extends LivingEntity {
    public PlayerMixin(EntityType<? extends LivingEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(
            method = "<init>",
            at = @At("TAIL")
    )
    private void onEquipStack(Level level, BlockPos pos, float yRot, GameProfile gameProfile, CallbackInfo ci) {
        if (level.isClientSide) {
            RenderSystem.recordRenderCall(() -> Vibrancy.ENTITY_LIGHTS.computeIfAbsent(this, RaytracedPointEntityLight::new));
        }
    }

    @Inject(
            method = "aiStep",
            at = @At("TAIL")
    )
    private void aiStep(CallbackInfo ci) {
        if (Vibrancy.ELYTRA_TRAILS.get() && isFallFlying() && level().isClientSide) {
            Vibrancy.elytraTrail(this);
        }
    }
}
