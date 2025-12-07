package net.typho.vibrancy.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.typho.vibrancy.api.BlockLight;
import net.typho.vibrancy.api.DynamicLightInfo;
import net.typho.vibrancy.api.UtilKt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Level.class)
public class LevelMixin {
    @Inject(
            method = "onBlockStateChange",
            at = @At("TAIL")
    )
    private void onBlockStateChange(BlockPos pos, BlockState oldBlock, BlockState newBlock, CallbackInfo ci) {
        DynamicLightInfo info = DynamicLightInfo.Companion.getMAP().get(UtilKt.getKey(newBlock.getBlock()));

        if (info != null) {
            info.addBlockLight(pos, newBlock);
        } else {
            BlockLight light = BlockLight.Companion.getLIGHTS().remove(pos);

            if (light != null) {
                light.free();
            }
        }
    }
}
