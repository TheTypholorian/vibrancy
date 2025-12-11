package net.typho.vibrancy.mixin;

import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(Options.class)
public class OptionsMixin {
    @ModifyArg(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/OptionInstance;createBoolean(Ljava/lang/String;Z)Lnet/minecraft/client/OptionInstance;",
                    ordinal = 6
            ),
            index = 1
    )
    private boolean init(boolean initialValue) {
        return false;
    }
}
