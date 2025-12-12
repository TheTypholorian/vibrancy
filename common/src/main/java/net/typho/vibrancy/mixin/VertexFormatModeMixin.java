package net.typho.vibrancy.mixin;

import com.mojang.blaze3d.vertex.VertexFormat;
import net.typho.vibrancy.Vibrancy;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;

import static org.lwjgl.opengl.GL40.GL_PATCHES;

@Mixin(VertexFormat.Mode.class)
public class VertexFormatModeMixin {
    @Shadow
    @Final
    @Mutable
    private static VertexFormat.Mode[] $VALUES;

    @Invoker("<init>")
    private static VertexFormat.Mode vibrancy$init(
            String name,
            int ordinal,
            int asGLMode,
            int primitiveLength,
            int primitiveStride,
            boolean connectedPrimitives
    ) {
        throw new UnsupportedOperationException();
    }

    static {
        VertexFormat.Mode[] arr = new VertexFormat.Mode[$VALUES.length + 1];
        System.arraycopy($VALUES, 0, arr, 0, $VALUES.length);
        Vibrancy.INSTANCE.setPATCHES_MODE(arr[$VALUES.length] = vibrancy$init(
                "VIBRANCY:PATCHES",
                $VALUES.length,
                GL_PATCHES,
                4,
                4,
                false
        ));
        $VALUES = arr;
    }
}
