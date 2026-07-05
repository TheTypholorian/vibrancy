package net.typho.vibrancy.mixin;

import net.minecraft.client.renderer.rendertype.PreparedRenderType;
import net.minecraft.resources.Identifier;
import net.typho.vibrancy.entity.PreparedRenderTypeTextureExtension;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(PreparedRenderType.Texture.class)
public class PreparedRenderTypeTextureMixin implements PreparedRenderTypeTextureExtension {
    @Unique
    private Identifier vibrancy$identifier = null;

    @Override
    @Nullable
    public Identifier getVibrancy$identifier() {
        return vibrancy$identifier;
    }

    @Override
    public void setVibrancy$identifier(@Nullable Identifier identifier) {
        vibrancy$identifier = identifier;
    }
}
