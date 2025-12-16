package net.typho.vibrancy.mixin;

import net.irisshaders.iris.targets.RenderTarget;
import net.typho.big_shot_lib.gl.resource.GlResourceType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = RenderTarget.class, remap = false)
public class RenderTargetMixin {
    @Shadow
    @Final
    private int mainTexture;

    @Shadow
    private String name;

    @Shadow
    @Final
    private int altTexture;

    @Inject(
            method = "<init>",
            at = @At("TAIL")
    )
    private void init(RenderTarget.Builder builder, CallbackInfo ci) {
        GlResourceType.Companion.getTEXTURE_2D().label(mainTexture, name);
        GlResourceType.Companion.getTEXTURE_2D().label(altTexture, name + "/alt");
    }
}
