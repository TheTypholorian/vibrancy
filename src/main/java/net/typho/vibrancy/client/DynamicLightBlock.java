package net.typho.vibrancy.client;

import foundry.veil.api.client.render.light.PointLight;
import net.minecraft.block.BlockState;

public interface DynamicLightBlock {
    PointLight createLight(BlockState state);
}
