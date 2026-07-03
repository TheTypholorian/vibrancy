package net.typho.vibrancy.mixin;

import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(TextureAtlasSprite.class)
public interface TextureAtlasSpriteAccessor {
    @Accessor("padding")
    int vibrancy$getPadding();

    @Invoker("<init>")
    static TextureAtlasSprite init(Identifier atlasLocation, SpriteContents contents, int atlasWidth, int atlasHeight, int x, int y, int padding) {
        return null;
    }
}
