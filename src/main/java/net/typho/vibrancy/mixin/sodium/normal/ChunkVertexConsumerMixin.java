package net.typho.vibrancy.mixin.sodium.normal;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.buffers.ChunkVertexConsumer;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder;
import net.typho.big_shot_lib.api.client.rendering.util.PackedNormal;
import net.typho.vibrancy.sodium.SodiumChunkVertexExtension;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChunkVertexConsumer.class)
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
        ((SodiumChunkVertexExtension) vertices[vertexIndex]).setVibrancy$normal(PackedNormal.pack(x, y, z));
    }
}
