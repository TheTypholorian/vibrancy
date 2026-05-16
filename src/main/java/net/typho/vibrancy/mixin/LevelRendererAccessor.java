package net.typho.vibrancy.mixin;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import dev.kikugie.fletching_table.annotation.MixinEnvironment;

//? if >=1.21.11 {
/*import net.minecraft.client.renderer.state.LevelRenderState;
*///? }

@MixinEnvironment(type = MixinEnvironment.Env.CLIENT)
@Mixin(LevelRenderer.class)
public interface LevelRendererAccessor {
    //? if >=1.21.11 {
    /*@Accessor("levelRenderState")
    LevelRenderState vibrancy$getLevelRenderState();
    *///? }

    @Accessor("visibleSections")
    ObjectArrayList<SectionRenderDispatcher.RenderSection> vibrancy$getVisibleSections();
}
