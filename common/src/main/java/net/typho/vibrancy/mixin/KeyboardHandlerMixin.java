package net.typho.vibrancy.mixin;

import foundry.veil.VeilClient;
import foundry.veil.impl.client.imgui.VeilImGuiImpl;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.typho.vibrancy.Vibrancy;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static org.lwjgl.glfw.GLFW.*;

@Mixin(KeyboardHandler.class)
public class KeyboardHandlerMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(
            method = "keyPress",
            at = @At("HEAD"),
            cancellable = true
    )
    public void keyPress(long window, int key, int scancode, int action, int mods, CallbackInfo ci) {
        if (window == minecraft.getWindow().getWindow()) {
            if (VeilClient.EDITOR_KEY.matches(key, scancode)) {
                if (action == GLFW_PRESS) {
                    ci.cancel();
                } else if (action == GLFW_RELEASE) {
                    VeilImGuiImpl.get().toggle();
                    ci.cancel();
                }
            } else if (action == GLFW_PRESS && glfwGetKey(window, GLFW_KEY_F6) == GLFW_PRESS) {
                if (Vibrancy.debugKey(key)) {
                    ci.cancel();
                }
            }
        }
    }
}
