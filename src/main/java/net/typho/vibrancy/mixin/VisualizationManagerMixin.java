package net.typho.vibrancy.mixin;

import dev.kikugie.fletching_table.annotation.MixinEnvironment;
import net.typho.vibrancy.Vibrancy;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@MixinEnvironment(type = MixinEnvironment.Env.CLIENT)
@Mixin(targets = "dev.engine_room.flywheel.api.visualization.VisualizationManager")
interface VisualizationManagerMixin {
    @Inject(
            method = "supportsVisualization",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void supportsVisualization(CallbackInfoReturnable<Boolean> cir) {
        if (Vibrancy.disableFlywheelInstancing) {
            cir.setReturnValue(false);
        }
    }
}
