package net.typho.vibrancy.client;

import foundry.veil.api.client.render.light.PointLight;
import net.minecraft.util.math.BlockPos;
import org.joml.Vector3dc;

public class RaytracedPointBlockLight extends RaytracedPointLight {
    public final BlockPos blockPos;

    public RaytracedPointBlockLight(BlockPos blockPos) {
        this.blockPos = blockPos;
        super.setPosition(blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5);
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
