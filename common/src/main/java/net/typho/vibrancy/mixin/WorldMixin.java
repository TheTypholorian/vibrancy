package net.typho.vibrancy.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.typho.vibrancy.Vibrancy;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Level.class)
public class WorldMixin {
    @Inject(
            method = "onBlockStateChange",
            at = @At("TAIL")
    )
    private void onBlockStateChange(BlockPos pos, BlockState oldBlock, BlockState newBlock, CallbackInfo ci) {
        Vibrancy.updateBlock(pos, newBlock);
    }
}
