package net.typho.vibrancy.mixin;

import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.textures.TextureFormat;
import dev.kikugie.fletching_table.annotation.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

//? if <1.21.5 {
/*import dev.kikugie.fletching_table.annotation.MixinIgnore;

@MixinIgnore
*///? }
@MixinEnvironment(type = MixinEnvironment.Env.CLIENT)
@Mixin(GlTexture.class)
public interface GlTextureAccessor {
    @Invoker("<init>")
    static GlTexture vibrancy$init(String label, TextureFormat format, int width, int height, int mipLevels, int id) {
        throw new IllegalStateException();
    }
}
