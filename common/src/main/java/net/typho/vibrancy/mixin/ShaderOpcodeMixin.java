package net.typho.vibrancy.mixin;

import net.typho.big_shot_lib.api.shaders.mixins.ShaderBytecodeBuffer;
import net.typho.big_shot_lib.api.shaders.mixins.ShaderMixinManager;
import net.typho.big_shot_lib.api.shaders.mixins.ShaderOpcode;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.nio.ByteBuffer;

@Mixin(value = ShaderOpcode.class, remap = false)
public class ShaderOpcodeMixin {
    @Shadow
    @Final
    public ShaderBytecodeBuffer parent;

    @Shadow
    @Final
    public int length;

    @Shadow
    @Final
    public int index;

    /**
     * @author
     * @reason
     */
    @Overwrite
    public final @NotNull String getString(int offset) {
        ByteBuffer buffer = ByteBuffer.allocate((length - offset - 1) * 4).order(ShaderMixinManager.BYTE_ORDER);
        int[] ints = new int[length - offset - 1];
        parent.buffer.get(index + offset + 1, ints);

        for (int i : ints) {
            buffer.putInt(i);
        }

        buffer.flip();
        return new String(buffer.array()).replaceAll(new String(new byte[]{0}), "");
    }
}
