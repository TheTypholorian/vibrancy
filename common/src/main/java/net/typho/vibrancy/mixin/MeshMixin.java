package net.typho.vibrancy.mixin;

import com.mojang.blaze3d.vertex.MeshData;
import net.typho.big_shot_lib.api.client.rendering.buffers.GlBuffer;
import net.typho.big_shot_lib.api.client.rendering.meshes.Mesh;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Mesh.class, remap = false)
public class MeshMixin {
    @Shadow
    @Final
    public GlBuffer ebo;

    @Inject(
            method = "upload",
            at = @At("TAIL")
    )
    private void upload(MeshData built, CallbackInfo ci) {
        ebo.unbind();
    }
}
