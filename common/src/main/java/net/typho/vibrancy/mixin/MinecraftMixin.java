package net.typho.vibrancy.mixin;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.main.GameConfig;
import net.minecraft.client.multiplayer.ClientLevel;
import net.typho.vibrancy.VibrancyDynamicBuffers;
import net.typho.vibrancy.light.BlockLight;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Shadow
    @Final
    private Window window;

    @Shadow
    @Final
    private RenderTarget mainRenderTarget;

    @Inject(
            method = "setLevel",
            at = @At("TAIL")
    )
    private void setLevel(ClientLevel level, ReceivingLevelScreen.Reason reason, CallbackInfo ci) {
        BlockLight.Companion.getLIGHTS().values().forEach(BlockLight::free);
        BlockLight.Companion.getLIGHTS().clear();
    }

    @Inject(
            method = "resizeDisplay",
            at = @At("TAIL")
    )
    private void resizeDisplay(CallbackInfo ci) {
        Objects.requireNonNull(VibrancyDynamicBuffers.normalsTexture).resize2D(window.getWidth(), window.getHeight());
        Objects.requireNonNull(VibrancyDynamicBuffers.albedoTexture).resize2D(window.getWidth(), window.getHeight());
        Objects.requireNonNull(VibrancyDynamicBuffers.lightUVTexture).resize2D(window.getWidth(), window.getHeight());
    }

    @Inject(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/pipeline/RenderTarget;setClearColor(FFFF)V"
            )
    )
    private void init(GameConfig gameConfig, CallbackInfo ci) {
        VibrancyDynamicBuffers.init(mainRenderTarget);
    }
}
