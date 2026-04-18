package net.typho.vibrancy.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.typho.big_shot_lib.api.client.rendering.util.NeoRenderSettings;
import net.typho.big_shot_lib.api.client.rendering.util.quad.NeoBakedQuad;
import net.typho.vibrancy.shadows.RaytracedGuiGraphics;
import net.typho.vibrancy.util.QuadListVertexConsumer;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Shadow
    @Final
    Minecraft minecraft;

    @WrapOperation(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/Screen;renderWithTooltip(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"
            )
    )
    private void render(
            Screen instance,
            GuiGraphics guiGraphics,
            int mouseX,
            int mouseY,
            float partialTick,
            Operation<Void> original
    ) {
        Map<NeoRenderSettings, List<NeoBakedQuad>> quads = new HashMap<>();

        try (RaytracedGuiGraphics raytraced = new RaytracedGuiGraphics(minecraft, guiGraphics.bufferSource()) {
            @Override
            public NeoBakedQuad.Consumer createConsumer(@NotNull NeoRenderSettings settings) {
                return new QuadListVertexConsumer(quads.computeIfAbsent(settings, key -> new ArrayList<>()));
            }
        }) {
            original.call(instance, raytraced, mouseX, mouseY, partialTick);
            raytraced.renderRaytraced();
        }
    }
}
