package net.typho.vibrancy.mixin;

import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.StagedVertexBuffer;
import net.typho.vibrancy.entity.StagedVertexBufferDrawExtension;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(StagedVertexBuffer.Draw.class)
public class StagedVertexBufferDrawMixin implements StagedVertexBufferDrawExtension {
    @Unique
    private VertexFormat vibrancy$originalFormat;

    @Override
    public @Nullable VertexFormat getVibrancy$originalFormat() {
        return vibrancy$originalFormat;
    }

    @Override
    public void setVibrancy$originalFormat(@Nullable VertexFormat vertexFormat) {
        vibrancy$originalFormat = vertexFormat;
    }
}
