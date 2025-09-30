package net.typho.vibrancy;

import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.dynamicbuffer.DynamicBufferType;
import foundry.veil.api.client.render.light.Light;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;
import org.lwjgl.system.NativeResource;

import java.util.Collection;
import java.util.Comparator;

public abstract class RaytracedLightRenderer<T extends Light & RaytracedLight> implements NativeResource {
    public int numRaytraced = 0, numVisible = 0;

    public void render() {
        int[] cap = {0};
        numRaytraced = 0;
        numVisible = 0;

        VeilRenderSystem.renderer().enableBuffers(Identifier.of(Vibrancy.MOD_ID, "mask"), DynamicBufferType.NORMAL);

        getLights().stream()
                .sorted(Comparator.comparingDouble(light -> light.lazyDistance(MinecraftClient.getInstance().gameRenderer.getCamera().getPos())))
                .filter(light -> {
                    boolean b = light.lazyDistance(MinecraftClient.getInstance().gameRenderer.getCamera().getPos()) / 16 < Vibrancy.LIGHT_CULL_DISTANCE.getValue() * Vibrancy.LIGHT_CULL_DISTANCE.getValue();

                    if (b) {
                        numVisible++;
                    }

                    return b;
                })
                .forEachOrdered(light -> {
                    boolean b = cap[0]++ < Vibrancy.maxLights();

                    if (b) {
                        numRaytraced++;
                    }

                    light.render(b);
                });

        VeilRenderSystem.renderer().disableBuffers(Identifier.of(Vibrancy.MOD_ID, "mask"));
    }

    public abstract Collection<? extends T> getLights();
}
