package net.typho.vibrancy.mixin.storage;

import net.caffeinemc.mods.sodium.client.gl.device.CommandList;
import net.caffeinemc.mods.sodium.client.render.chunk.region.RenderRegion;
import net.typho.vibrancy.storage.BlockLightStorage;
import net.typho.vibrancy.ext.RenderRegionExt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderRegion.class)
public class RenderRegionMixin implements RenderRegionExt {
    @Unique
    private final BlockLightStorage vibrancy$blockLightStorage = new BlockLightStorage((RenderRegion) (Object) this);

    @Override
    public BlockLightStorage vibrancy$getBlockLightStorage() {
        return vibrancy$blockLightStorage;
    }

    @Inject(
            method = "delete(Lnet/caffeinemc/mods/sodium/client/gl/device/CommandList;)V",
            at = @At("TAIL")
    )
    private void delete(
            CommandList commandList,
            CallbackInfo ci
    ) {
        vibrancy$blockLightStorage.close();
    }
}
