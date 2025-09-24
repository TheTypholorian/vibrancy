package net.typho.vibrancy.client;

import foundry.veil.api.client.render.CullFrustum;
import foundry.veil.api.client.render.light.Light;
import foundry.veil.api.client.render.light.renderer.LightRenderer;
import foundry.veil.api.client.render.light.renderer.LightTypeRenderer;
import org.lwjgl.system.NativeResource;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public abstract class RaytracedLightRenderer<T extends Light & RaytracedLight> implements LightTypeRenderer<T> {
    private final List<T> lights = new LinkedList<>();

    @Override
    public void prepareLights(LightRenderer lightRenderer, List<T> lights, Set<T> removedLights, CullFrustum frustum) {
        VibrancyClient.DYNAMIC_LIGHT_INFOS.clear();
        this.lights.clear();

        for (T light : lights) {
            light.prepare(lightRenderer, frustum);
            this.lights.add(light);
        }

        for (T light : removedLights) {
            light.free();
        }
    }

    @Override
    public void renderLights(LightRenderer lightRenderer, List<T> lights) {
        for (T light : lights) {
            light.render(lightRenderer);
        }
    }

    @Override
    public int getVisibleLights() {
        return (int) lights.stream().filter(RaytracedLight::isVisible).count();
    }

    @Override
    public void free() {
        lights.forEach(NativeResource::close);
    }
}
