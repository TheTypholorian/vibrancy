package net.typho.vibrancy;

import foundry.veil.api.client.render.light.PointLight;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3dc;

public class RaytracedPointBlockLight extends RaytracedPointLight {
    public final BlockPos blockPos;
    public final Block block;
    public final DynamicLightInfo info;

    public RaytracedPointBlockLight(BlockPos blockPos, Block block, DynamicLightInfo info, Vec3d offset) {
        this.blockPos = blockPos;
        this.block = block;
        this.info = info;
        super.setPosition(blockPos.getX() + offset.x, blockPos.getY() + offset.y, blockPos.getZ() + offset.z);
    }

    @Override
    public void render(boolean raytrace) {
        if (MinecraftClient.getInstance().world.getBlockState(blockPos).getBlock() == block) {
            super.render(raytrace);
        } else {
            remove = true;
        }
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
        return getClass().getSimpleName() + blockPos;
    }
}
