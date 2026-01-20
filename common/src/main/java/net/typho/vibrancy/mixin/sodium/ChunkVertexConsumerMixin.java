package net.typho.vibrancy.mixin.sodium;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.caffeinemc.mods.sodium.api.util.NormI8;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.buffers.ChunkVertexConsumer;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder;
import net.typho.vibrancy.sodium.VertexWithNormal;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ChunkVertexConsumer.class, remap = false)
public class ChunkVertexConsumerMixin {
    @Shadow
    @Final
    private ChunkVertexEncoder.Vertex[] vertices;

    @Shadow
    private int vertexIndex;

    @Inject(
            method = "setNormal",
            at = @At("HEAD")
    )
    private void setNormal(float x, float y, float z, CallbackInfoReturnable<VertexConsumer> cir) {
        ((VertexWithNormal) vertices[vertexIndex]).vibrancy$setNormal(NormI8.pack(x, y, z));
    }
}
