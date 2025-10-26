package net.typho.vibrancy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;

public record BlockBox(BlockPos min, BlockPos max) {
    public static BlockBox of(BlockPos pos) {
        return new BlockBox(pos, pos);
    }

    public boolean contains(Vec3i pos) {
        return min.getX() <= pos.getX() && max.getX() >= pos.getX() &&
                min.getY() <= pos.getY() && max.getY() >= pos.getY() &&
                min.getZ() <= pos.getZ() && max.getZ() >= pos.getZ();
    }
}
