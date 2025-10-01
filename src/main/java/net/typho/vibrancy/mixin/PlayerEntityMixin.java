package net.typho.vibrancy.mixin;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.typho.vibrancy.RaytracedPointEntityLight;
import net.typho.vibrancy.Vibrancy;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public class PlayerEntityMixin {
    @Inject(
            method = "<init>",
            at = @At("TAIL")
    )
    private void onEquipStack(World world, BlockPos pos, float yaw, GameProfile gameProfile, CallbackInfo ci) {
        if (world.isClient) {
            RenderSystem.recordRenderCall(() -> Vibrancy.ENTITY_LIGHTS.put((LivingEntity) (Object) this, new RaytracedPointEntityLight((LivingEntity) (Object) this)));
        }
    }
}
