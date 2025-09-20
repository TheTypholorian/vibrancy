package net.typho.vibrancy.mixin.client;

import foundry.veil.api.client.render.shader.compiler.VeilShaderSource;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(value = VeilShaderSource.class, remap = false)
public class VeilShaderSourceMixin {
    @Shadow
    @Final
    @Mutable
    private String sourceCode;

    @Inject(
            method = "<init>(Lnet/minecraft/util/Identifier;Ljava/lang/String;Lit/unimi/dsi/fastutil/objects/Object2IntMap;Ljava/util/Set;Ljava/util/Set;)V",
            at = @At("TAIL")
    )
    private void code(Identifier sourceId, String s, Object2IntMap<String> uniformBindings, Set<String> definitionDependencies, Set<Identifier> includes, CallbackInfo ci) {
        if (sourceId != null && sourceId.getNamespace().equals("veil") && sourceId.getPath().equals("light/indirect_sphere")) {
            sourceCode = sourceCode.replace("readonly", "").replace("writeonly", "");
        }
    }
}

