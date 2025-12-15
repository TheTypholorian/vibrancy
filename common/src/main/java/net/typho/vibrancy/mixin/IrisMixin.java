package net.typho.vibrancy.mixin;

import net.irisshaders.iris.Iris;
import net.irisshaders.iris.pipeline.VanillaRenderingPipeline;
import net.typho.vibrancy.VibrancyRenderingPipeline;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = Iris.class, remap = false)
public class IrisMixin {
    @Redirect(
            method = "createPipeline",
            at = @At(
                    value = "NEW",
                    target = "net/irisshaders/iris/pipeline/VanillaRenderingPipeline"
            )
    )
    private static VanillaRenderingPipeline createPipeline() {
        return new VibrancyRenderingPipeline();
    }
}
