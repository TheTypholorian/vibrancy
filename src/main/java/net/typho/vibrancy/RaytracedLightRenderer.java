package net.typho.vibrancy;

import foundry.veil.api.client.render.light.Light;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.system.NativeResource;

import java.util.Collection;
import java.util.Comparator;

public abstract class RaytracedLightRenderer<T extends Light & RaytracedLight> implements NativeResource {
    public void render() {
        Vibrancy.blitViewPos();
        int[] cap = {0};

        getLights().stream()
                .sorted(Comparator.comparingDouble(light -> light.lazyDistance(MinecraftClient.getInstance().gameRenderer.getCamera().getPos())))
                .forEachOrdered(light -> light.render(cap[0]++ < Vibrancy.maxLights()));
    }

    public abstract Collection<? extends T> getLights();
}
