package net.typho.vibrancy.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.typho.vibrancy.Vibrancy;

public class VibrancyClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ResourceManagerHelper.registerBuiltinResourcePack(Identifier.of(Vibrancy.MOD_ID, "vibrancy"), FabricLoader.getInstance().getModContainer(Vibrancy.MOD_ID).orElseThrow(), Text.translatable("pack.name.vibrancy"), ResourcePackActivationType.DEFAULT_ENABLED);
    }
}
