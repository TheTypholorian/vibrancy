package net.typho.vibrancy.mixin;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.caffeinemc.mods.sodium.client.render.vertex.VertexConsumerTracker;
import net.typho.vibrancy.shadows.ShadowBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VertexConsumerTracker.class)
public class VertexConsumerTrackerMixin {
    @Inject(
            method = "logBadConsumer",
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/slf4j/Logger;warn(Ljava/lang/String;Ljava/lang/Object;)V"
            ),
            cancellable = true
    )
    private static void logBadConsumer(VertexConsumer consumer, CallbackInfo ci) {
        if (consumer instanceof ShadowBuilder) {
            ci.cancel();
        }
    }
}
