package net.typho.vibrancy.mixin;

import net.typho.big_shot_lib.api.shaders.mixins.ShaderBytecodeBuffer;
import net.typho.big_shot_lib.api.shaders.mixins.ShaderOpcode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(value = ShaderBytecodeBuffer.class, remap = false)
public class ShaderBytecodeBufferMixin {
    @ModifyArg(
            method = "findVariable",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/typho/big_shot_lib/api/shaders/mixins/ShaderBytecodeBuffer;findOpcode(I[Lkotlin/Pair;)Lnet/typho/big_shot_lib/api/shaders/mixins/ShaderOpcode;",
                    ordinal = 1
            )
    )
    private int findVariable1(int id) {
        return ShaderOpcode.OP_DECORATE;
    }

    /*
    @ModifyArg(
            method = "findVariable",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/typho/big_shot_lib/api/shaders/mixins/ShaderBytecodeBuffer;findOpcode(I[Lkotlin/Pair;)Lnet/typho/big_shot_lib/api/shaders/mixins/ShaderOpcode;",
                    ordinal = 1
            ),
            index = 1
    )
    private Pair<Integer, Integer>[] findVariable2(Pair<Integer, Integer>[] expectedValues) {
        return Arrays.stream(expectedValues).map(pair -> new Pair<Integer, Integer>(pair.getFirst() + 1, pair.getSecond())).toArray(Pair[]::new);
    }
     */
}
