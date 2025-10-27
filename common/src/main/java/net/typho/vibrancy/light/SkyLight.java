package net.typho.vibrancy.light;

import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

public class SkyLight implements RaytracedLight {
    public static @Nullable SkyLight INSTANCE;

    @Override
    public void updateDirty(Iterable<BlockPos> it) {
    }

    @Override
    public void init() {
    }

    @Override
    public boolean render(boolean raytrace) {
        return false;
    }

    @Override
    public Vector3d getPosition() {
        return null;
    }

    @Override
    public boolean shouldRender() {
        return true;
    }

    @Override
    public void free() {
    }
}
