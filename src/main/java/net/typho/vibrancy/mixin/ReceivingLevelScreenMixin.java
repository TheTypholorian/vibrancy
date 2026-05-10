package net.typho.vibrancy.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.typho.vibrancy.Vibrancy;
import net.typho.vibrancy.block.impl.RayPointLightStorage;
import net.typho.vibrancy.block.impl.RayPointLightType;
import net.typho.vibrancy.block.impl.SubtleLightStorage;
import net.typho.vibrancy.block.impl.SubtleLightType;
import net.typho.vibrancy.sky.impl.OverworldSkyLightStorage;
import net.typho.vibrancy.util.VibrancyThreadPool;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

//? if <1.21 {
/*import org.spongepowered.asm.mixin.Unique;
*///? }

@Mixin(ReceivingLevelScreen.class)
public abstract class ReceivingLevelScreenMixin extends Screen {
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

    protected ReceivingLevelScreenMixin(Component component) {
        super(component);
    }

    @Inject(
            method = "render",
            at = @At("TAIL")
    )
    private void render(GuiGraphics guiGraphics, int i, int j, float f, CallbackInfo ci) {
        if (levelReceived.getAsBoolean()) {
            int y = height / 2 - 50 + font.lineHeight * 2;
            var rayPointLights = Vibrancy.lightManager.blockLights.get(RayPointLightType.INSTANCE);

            if (rayPointLights != null) {
                var loading = ((RayPointLightStorage) rayPointLights).getMap().values().stream().filter(light -> light.mesh.isTaskActive()).count();
                var max = rayPointLights.getSize();
                guiGraphics.drawCenteredString(font, Component.translatable("loading.vibrancy.raytraced_point", loading == 0 ? CommonComponents.GUI_DONE : (max - loading) + " / " + max), width / 2, y, 16777215);
                y += font.lineHeight;
            }

            var subtleLights = Vibrancy.lightManager.blockLights.get(SubtleLightType.INSTANCE);

            if (subtleLights != null) {
                var loading = ((SubtleLightStorage) subtleLights).chunks.values().stream().filter(chunk -> chunk.getTask() != null).count();
                var max = ((SubtleLightStorage) subtleLights).chunks.size();
                guiGraphics.drawCenteredString(font, Component.translatable("loading.vibrancy.subtle", loading == 0 ? CommonComponents.GUI_DONE : (max - loading) + " / " + max), width / 2, y, 16777215);
                y += font.lineHeight;
            }

            var skyLight = Vibrancy.lightManager.skyLight;

            if (skyLight != null && skyLight.getSecond() instanceof OverworldSkyLightStorage storage) {
                var loading = storage.chunks.values().stream().filter(OverworldSkyLightStorage.Chunk::isTaskActive).count();
                var max = storage.chunks.size();
                guiGraphics.drawCenteredString(font, Component.translatable("loading.vibrancy.overworld", loading == 0 ? CommonComponents.GUI_DONE : (max - loading) + " / " + max), width / 2, y, 16777215);
                y += font.lineHeight;
            }

            y += font.lineHeight;
            guiGraphics.drawCenteredString(font, Component.translatable("loading.vibrancy.timeout", (createdAt + 15000L - System.currentTimeMillis()) / 1000), width / 2, y, 16777215);

            y += font.lineHeight * 2;
            guiGraphics.drawCenteredString(font, Component.translatable("loading.vibrancy.tip1"), width / 2, y, 16777215);
            y += font.lineHeight;
            guiGraphics.drawCenteredString(font, Component.translatable("loading.vibrancy.tip2"), width / 2, y, 16777215);
        }
    }

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
