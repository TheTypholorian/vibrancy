package net.typho.vibrancy.client;

import foundry.veil.api.client.render.light.PointLight;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3dc;

public class RaytracedPointBlockLight extends RaytracedPointLight {
    public final BlockPos blockPos;

    public RaytracedPointBlockLight(BlockPos blockPos, Vec3d offset) {
        this.blockPos = blockPos;
        super.setPosition(blockPos.getX() + offset.x, blockPos.getY() + offset.y, blockPos.getZ() + offset.z);
    }

    @Override
    public PointLight setPosition(Vector3dc position) {
        throw new UnsupportedOperationException("Can't move a block light");
    }

    @Override
    public PointLight setPosition(double x, double y, double z) {
        throw new UnsupportedOperationException("Can't move a block light");
    }
}
