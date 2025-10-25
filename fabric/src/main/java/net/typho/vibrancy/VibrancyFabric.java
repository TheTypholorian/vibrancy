package net.typho.vibrancy;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.resources.model.BlockStateModelLoader;

public class VibrancyFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Vibrancy.BLOCK_STATE_PREDICATE = BlockStateModelLoader::predicate;
        Vibrancy.init();
    }
}
