package net.typho.vibrancy.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.util.Identifier;
import net.minecraft.util.InvalidIdentifierException;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = {"foundry.veil.impl.client.render.dynamicbuffer.VanillaShaderCompiler$1", "net.minecraft.client.gl.ShaderProgram$1"})
public class VanillaShaderCompilerMixin {
    @WrapOperation(
            method = "loadImport",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/Identifier;of(Ljava/lang/String;)Lnet/minecraft/util/Identifier;",
                    ordinal = 0
            )
    )
    private Identifier of(String id, Operation<Identifier> original) {
        if (id.indexOf(Identifier.NAMESPACE_SEPARATOR) != -1) {
            try {
                return original.call(id).withPrefixedPath("pinwheel/");
            } catch (InvalidIdentifierException e) {
                int i = id.indexOf(Identifier.NAMESPACE_SEPARATOR);
                String prefix = id.substring(0, i);
                String namespace = prefix.substring(prefix.lastIndexOf('/') + 1);
                String path = prefix.substring(0, prefix.lastIndexOf('/') + 1) + id.substring(i + 1);
                return Identifier.of(namespace, "pinwheel/" + path);
            }
        }

        return original.call(id);
    }
}
