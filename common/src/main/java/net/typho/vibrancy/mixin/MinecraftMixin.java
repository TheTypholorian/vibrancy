package net.typho.vibrancy.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.typho.big_shot_lib.api.services.WrapperUtil;
import net.typho.vibrancy.Vibrancy;
import net.typho.vibrancy.block.BlockLightInfoLoader;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Shadow
    @Final
    private ReloadableResourceManager resourceManager;

    @Inject(
            method = "setLevel",
            at = @At("TAIL")
    )
    private void setLevel(ClientLevel level, ReceivingLevelScreen.Reason reason, CallbackInfo ci) {
        Vibrancy.LIGHT_MANAGER.clear();

        BlockLightInfoLoader.load(WrapperUtil.INSTANCE.wrap(resourceManager), WrapperUtil.INSTANCE.wrap(level.registryAccess()));
    }
}
