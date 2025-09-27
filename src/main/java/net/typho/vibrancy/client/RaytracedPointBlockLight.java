package net.typho.vibrancy.client;

import foundry.veil.api.client.render.CullFrustum;
import foundry.veil.api.client.render.light.PointLight;
import foundry.veil.api.client.render.light.renderer.LightRenderer;
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
    public void prepare(LightRenderer renderer, CullFrustum frustum) {
        if (MinecraftClient.getInstance().world.getBlockState(blockPos).getBlock() == block) {
            super.prepare(renderer, frustum);
        } else {
            remove = true;
        }
    }

    @Override
    public PointLight setPosition(Vector3dc position) {
        throw new UnsupportedOperationException("Can't move a block light");
    }

    @Override
    public PointLight setPosition(double x, double y, double z) {
        throw new UnsupportedOperationException("Can't move a block light");
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + blockPos;
    }
}
