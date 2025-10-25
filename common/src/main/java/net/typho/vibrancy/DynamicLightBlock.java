package net.typho.vibrancy;

import foundry.veil.api.client.render.light.PointLight;
import net.minecraft.block.BlockState;

public interface DynamicLightBlock {
    PointLight createLight(BlockState state);
}
