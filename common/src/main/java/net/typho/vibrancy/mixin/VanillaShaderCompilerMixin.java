package net.typho.vibrancy.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.util.InvalidResourceLocationException;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = {"foundry.veil.impl.client.render.dynamicbuffer.VanillaShaderCompiler$1", "net.minecraft.client.gl.ShaderProgram$1"})
public class VanillaShaderCompilerMixin {
    @WrapOperation(
            method = "loadImport",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/ResourceLocation;of(Ljava/lang/String;)Lnet/minecraft/util/ResourceLocation;",
                    ordinal = 0
            )
    )
    private ResourceLocation of(String id, Operation<ResourceLocation> original) {
        if (id.indexOf(ResourceLocation.NAMESPACE_SEPARATOR) != -1) {
            try {
                return original.call(id).withPrefixedPath("pinwheel/");
            } catch (InvalidResourceLocationException e) {
                int i = id.indexOf(ResourceLocation.NAMESPACE_SEPARATOR);
                String prefix = id.substring(0, i);
                String namespace = prefix.substring(prefix.lastIndexOf('/') + 1);
                String path = prefix.substring(0, prefix.lastIndexOf('/') + 1) + id.substring(i + 1);
                return ResourceLocation.of(namespace, "pinwheel/" + path);
            }
        }

        return original.call(id);
    }
}
