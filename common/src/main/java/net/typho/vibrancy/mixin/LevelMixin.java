package net.typho.vibrancy.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.typho.vibrancy.Vibrancy;
import net.typho.vibrancy.block.BlockLight;
import net.typho.vibrancy.block.BlockLightInfo;
import net.typho.vibrancy.block.BlockLightRegistry;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Level.class)
public class LevelMixin {
    @Shadow
    @Final
    private ResourceKey<Level> dimension;

    @Inject(
            method = "onBlockStateChange",
            at = @At("TAIL")
    )
    private void onBlockStateChange(BlockPos pos, BlockState oldBlock, BlockState newBlock, CallbackInfo ci) {
        if (Vibrancy.config.blockLights.enabled) {
            BlockLightInfo info = BlockLightRegistry.get(newBlock.getBlock());

            if (info != null) {
                info.addBlockLight(Vibrancy.LIGHT_MANAGER, newBlock, pos);
            } else {
                BlockLight<?> light = Vibrancy.LIGHT_MANAGER.getBlockLights().remove(pos);

                if (light != null) {
                    light.close();
                }
            }
        }

        Vibrancy.LIGHT_MANAGER.getDirtyBlocks().add(new GlobalPos(dimension, pos));
    }
}
