package net.typho.vibrancy.mixin.storage;

import com.llamalad7.mixinextras.sugar.Local;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSection;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSectionManager;
import net.caffeinemc.mods.sodium.client.render.chunk.UniformBufferManager;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.BuilderTaskOutput;
import net.caffeinemc.mods.sodium.client.render.viewport.Viewport;
import net.typho.vibrancy.Vibrancy;
import net.typho.vibrancy.ext.RenderRegionExt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(RenderSectionManager.class)
public class RenderSectionManagerMixin {
    @Inject(
            method = "processChunkBuildResults",
            at = @At("TAIL")
    )
    private void updateLights(
            ArrayList<BuilderTaskOutput> results,
            Viewport viewport,
            UniformBufferManager uniforms,
            CallbackInfoReturnable<Integer> cir,
            @Local(name = "sectionsWithOutputs") List<RenderSection> sectionsWithOutputs
    ) {
        sectionsWithOutputs.forEach(section -> Vibrancy.onSectionChanged(section));
    }
}
