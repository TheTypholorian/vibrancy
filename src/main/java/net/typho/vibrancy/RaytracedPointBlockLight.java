package net.typho.vibrancy;

import foundry.veil.api.client.render.light.PointLight;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3dc;

public class RaytracedPointBlockLight extends RaytracedPointLight {
    public final BlockPos blockPos;
    protected boolean render = false;

    public RaytracedPointBlockLight(BlockPos blockPos) {
        this.blockPos = blockPos;
    }

    @Override
    public void init() {
        render = false;

        boolean dirty = isDirty();

        ClientWorld world = MinecraftClient.getInstance().world;

        if (world != null) {
            BlockState state = world.getBlockState(blockPos);
            DynamicLightInfo info = DynamicLightInfo.get(state);

            if (info != null) {
                Vec3d offset = info.offset().apply(state).orElse(new Vec3d(0.5, 0.5, 0.5));
                position.set(blockPos.getX() + offset.x, blockPos.getY() + offset.y, blockPos.getZ() + offset.z);
                info.initLight(this, state);
                render = true;
            }
        }

        if (dirty) {
            markDirty();
        } else {
            clean();
        }
    }

    @Override
    public boolean render(boolean raytrace) {
        if (!render) {
            return false;
        }

        return super.render(raytrace);
    }

    @Override
    public final PointLight setPosition(Vector3dc position) {
        throw new UnsupportedOperationException("Can't move a block light");
    }

    @Override
    public final PointLight setPosition(double x, double y, double z) {
        throw new UnsupportedOperationException("Can't move a block light");
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + blockPos.getX() + ", " + blockPos.getY() + ", " + blockPos.getZ() + "]";
    }
}
