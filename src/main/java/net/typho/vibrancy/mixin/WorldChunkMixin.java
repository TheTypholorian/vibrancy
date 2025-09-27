package net.typho.vibrancy.mixin;

import foundry.veil.api.client.render.VeilRenderSystem;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.WorldChunk;
import net.typho.vibrancy.RaytracedPointLight;
import net.typho.vibrancy.Vibrancy;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WorldChunk.class)
public class WorldChunkMixin {
    @Inject(
            method = "setBlockState",
            at = @At(
                    value = "RETURN",
                    ordinal = 3
            )
    )
    private void setBlockState(BlockPos pos, BlockState state, boolean moved, CallbackInfoReturnable<BlockState> cir) {
        MinecraftClient.getInstance().execute(() -> {
            for (RaytracedPointLight light : VeilRenderSystem.renderer().getLightRenderer().getLights(Vibrancy.RAY_POINT_LIGHT.get())) {
                if (light.getPosition().distanceSquared(new Vector3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)) <= light.getRadius() * light.getRadius()) {
                    light.markDirty();
                }
            }
        });
    }
}
