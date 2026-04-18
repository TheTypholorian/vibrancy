package net.typho.vibrancy.mixin;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.typho.vibrancy.block.BlockLightInfo;
import net.typho.vibrancy.block.BlockLightRegistry;
import net.typho.vibrancy.shadows.RaytracedGuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiGraphics.class)
public class GuiGraphicsMixin {
    @Inject(
            method = "renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;IIII)V",
            at = @At("HEAD")
    )
    private void renderItem(LivingEntity entity, Level level, ItemStack stack, int x, int y, int seed, int guiOffset, CallbackInfo ci) {
        if ((Object) this instanceof RaytracedGuiGraphics raytraced) {
            if (stack.getItem() instanceof BlockItem blockItem) {
                BlockLightInfo info = BlockLightRegistry.blockMap.get(blockItem.getBlock());

                if (info != null) {
                    raytraced.lights.add(new RaytracedGuiGraphics.Light(x, y, stack, info));
                }
            }
        }
    }
}
