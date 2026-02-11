package net.typho.vibrancy.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.typho.big_shot_lib.api.shaders.mixins.ShaderBytecodeBuffer;
import net.typho.big_shot_lib.api.shaders.mixins.ShaderMixinManager;
import net.typho.big_shot_lib.api.shaders.mixins.ShaderOpcode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Mixin(targets = "net.typho.big_shot_lib.api.shaders.mixins.ShaderBytecodeBuffer$opcodes$1$iterator$1", remap = false)
public class ShaderBytecodeBufferIteratorMixin {
    @Shadow
    private int index;

    @Shadow
    private ShaderBytecodeBuffer this$0;

    @Inject(
            method = "next",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/typho/big_shot_lib/api/shaders/mixins/ShaderOpcode;<init>(Lnet/typho/big_shot_lib/api/shaders/mixins/ShaderBytecodeBuffer;III)V"
            )
    )
    private void next(
            CallbackInfoReturnable<ShaderOpcode> ci,
            @Local(name = "length") int length
    ) throws IOException {
        if (length <= 0) {
            Path path = Paths.get("debug.bin");

            ByteBuffer buffer = ByteBuffer.allocate(this$0.buffer.capacity() * 4).order(ShaderMixinManager.BYTE_ORDER);
            int[] ints = new int[this$0.buffer.capacity()];
            this$0.buffer.get(0, ints);

            for (int i : ints) {
                buffer.putInt(i);
            }

            buffer.flip();

            Files.write(path, buffer.array());

            throw new IllegalStateException("Illegal opcode length " + length + " at index " + index);
        }
    }
}
