package net.typho.vibrancy.mixin.client;

import net.minecraft.client.gl.GlDebug;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GlDebug.class)
public class GlDebugMixin {
    @Shadow
    @Final
    private static Logger LOGGER;

    @Inject(
            method = "info",
            at = @At("TAIL")
    )
    private static void info(int source, int type, int id, int severity, int messageLength, long message, long l, CallbackInfo ci) {
        LOGGER.info("at {}", Thread.currentThread().getStackTrace()[6]);
    }
}
