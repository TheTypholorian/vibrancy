package net.typho.vibrancy.client;

import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;

public record DynamicLightInfo(BlockPos pos, int groups, int avGSize, int numQuads) {
    @Override
    public @NotNull String toString() {
        return "X: " + pos.getX() + " Y: " + pos.getY() + " Z: " + pos.getZ() + " Groups: " + groups + " AvSize: " + avGSize + " NumQuads: " + numQuads;
    }
}
