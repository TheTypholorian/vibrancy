package net.typho.vibrancy.mixin;

import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import dev.kikugie.fletching_table.annotation.MixinEnvironment;

//? if >=1.21.11 {
/*import net.minecraft.client.renderer.state.LevelRenderState;
import org.spongepowered.asm.mixin.gen.Accessor;
*///? }

//? if <1.21.11 {
import dev.kikugie.fletching_table.annotation.MixinIgnore;

@MixinIgnore
//? }
@MixinEnvironment(type = MixinEnvironment.Env.CLIENT)
@Mixin(LevelRenderer.class)
public interface LevelRendererAccessor {
    //? if >=1.21.11 {
    /*@Accessor("levelRenderState")
    LevelRenderState vibrancy$getLevelRenderState();
    *///? }
}
