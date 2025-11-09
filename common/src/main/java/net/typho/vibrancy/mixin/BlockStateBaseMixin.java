package net.typho.vibrancy.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.typho.vibrancy.BlockStateFunction;
import net.typho.vibrancy.Vibrancy;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateBaseMixin {
    @Shadow
    public abstract Block getBlock();

    @Shadow
    protected abstract BlockState asState();

    @Inject(
            method = "emissiveRendering",
            at = @At("HEAD"),
            cancellable = true
    )
    @SuppressWarnings("deprecation")
    private void emissiveRendering(BlockGetter level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        BlockStateFunction<Boolean> func = Vibrancy.EMISSIVE_OVERRIDES.get(getBlock().builtInRegistryHolder().key());

        if (func != null) {
            cir.setReturnValue(func.apply(asState()));
        }
    }
}
