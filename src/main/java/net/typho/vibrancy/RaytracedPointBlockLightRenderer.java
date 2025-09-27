package net.typho.vibrancy;

import net.minecraft.util.math.BlockPos;
import org.lwjgl.system.NativeResource;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class RaytracedPointBlockLightRenderer extends RaytracedLightRenderer<RaytracedPointBlockLight> {
    public static final RaytracedPointBlockLightRenderer INSTANCE = new RaytracedPointBlockLightRenderer();
    public final Map<BlockPos, RaytracedPointBlockLight> lights = new LinkedHashMap<>();

    @Override
    public void render() {
        lights.values().removeIf(light -> light == null || light.remove);
        super.render();
    }

    @Override
    public Collection<RaytracedPointBlockLight> getLights() {
        return lights.values();
    }

    @Override
    public void free() {
        lights.values().forEach(NativeResource::free);
    }
}
