package net.typho.vibrancy.mixin;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.caffeinemc.mods.sodium.api.util.NormI8;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.buffers.ChunkVertexConsumer;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder;
import net.typho.vibrancy.sodium.ChunkVertexEncoderVertexExtension;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author Developers of Veil
 */
@Pseudo
@Mixin(value = ChunkVertexConsumer.class, remap = false)
public class ChunkVertexConsumerMixin {
    @Shadow
    @Final
    private ChunkVertexEncoder.Vertex[] vertices;

    @Shadow
    private int vertexIndex;

    @Inject(method = "setNormal", at = @At("HEAD"))
    public void setNormal(float x, float y, float z, CallbackInfoReturnable<VertexConsumer> cir) {
        ((ChunkVertexEncoderVertexExtension) vertices[vertexIndex]).vibrancy$setNormal(NormI8.pack(x, y, z));
    }
}
