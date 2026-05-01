package net.typho.vibrancy.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.network.chat.Component;
import net.typho.vibrancy.util.VibrancyThreadPool;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.function.BooleanSupplier;

//? if <1.21 {
/*import org.spongepowered.asm.mixin.Unique;
*///? }

@Mixin(ReceivingLevelScreen.class)
public class ReceivingLevelScreenMixin {
    @Shadow
    @Final
    private long createdAt;

    //? if <1.21 {
    /*@Unique
    private boolean vibrancy$levelReceived;

    @ModifyArg(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;drawCenteredString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)V"
            ),
            index = 1
    )
    private Component render(Component text) {
        return vibrancy$levelReceived ? Component.translatable("vibrancy.loading") : text;
    }

    @WrapOperation(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/ReceivingLevelScreen;onClose()V"
            )
    )
    private void tick(ReceivingLevelScreen instance, Operation<Void> original) {
        if (VibrancyThreadPool.INSTANCE.getQueue().size() < 10 || System.currentTimeMillis() > createdAt + 15000L) {
            original.call(instance);
        } else {
            vibrancy$levelReceived = true;
        }
    }

    *///? } else {
    
    @Shadow
    @Final
    private BooleanSupplier levelReceived;

    @ModifyArg(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;drawCenteredString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)V"
            ),
            index = 1
    )
    private Component render(Component text) {
        return levelReceived.getAsBoolean() ? Component.translatable("vibrancy.loading") : text;
    }

    @WrapOperation(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/function/BooleanSupplier;getAsBoolean()Z"
            )
    )
    private boolean tick(BooleanSupplier instance, Operation<Boolean> original) {
        return original.call(instance) && (VibrancyThreadPool.INSTANCE.getQueue().size() < 10 || System.currentTimeMillis() > createdAt + 15000L);
    }
    //? }
}
