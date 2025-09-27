package net.typho.vibrancy.mixin;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.typho.vibrancy.BlockStateFunction;
import net.typho.vibrancy.Vibrancy;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractBlock.AbstractBlockState.class)
public abstract class AbstractBlockStateMixin {
    @Shadow
    public abstract Block getBlock();

    @Shadow
    protected abstract BlockState asBlockState();

    @Inject(
            method = "hasEmissiveLighting",
            at = @At("HEAD"),
            cancellable = true
    )
    @SuppressWarnings("deprecation")
    private void hasEmissiveLighting(BlockView world, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        BlockStateFunction<Boolean> func = Vibrancy.EMISSIVE_OVERRIDES.get(getBlock().getRegistryEntry().registryKey());

        if (func != null) {
            cir.setReturnValue(func.apply(asBlockState()));
        }
    }
}
