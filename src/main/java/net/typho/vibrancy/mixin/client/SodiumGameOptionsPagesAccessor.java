package net.typho.vibrancy.mixin.client;

import net.caffeinemc.mods.sodium.client.gui.SodiumGameOptionPages;
import net.caffeinemc.mods.sodium.client.gui.options.storage.MinecraftOptionsStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = SodiumGameOptionPages.class, remap = false)
public interface SodiumGameOptionsPagesAccessor {
    @Accessor("vanillaOpts")
    static MinecraftOptionsStorage vanillaOpts() {
        throw new IllegalStateException();
    }
}
