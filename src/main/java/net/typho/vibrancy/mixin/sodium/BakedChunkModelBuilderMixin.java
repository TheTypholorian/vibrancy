package net.typho.vibrancy.mixin.sodium;

import net.caffeinemc.mods.sodium.client.render.chunk.compile.buffers.BakedChunkModelBuilder;
import net.typho.big_shot_lib.api.client.rendering.util.quad.NeoBakedQuad;
import net.typho.vibrancy.shadows.BlockFace;import net.typho.vibrancy.util.SectionMeshCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(BakedChunkModelBuilder.class)
public class BakedChunkModelBuilderMixin implements SectionMeshCache.ConsumerExtension {
    @Unique
    private BlockFace.Consumer vibrancy$sectionMeshConsumer;

    @Override
    public BlockFace.Consumer getVibrancy$sectionMeshConsumer() {
        return vibrancy$sectionMeshConsumer;
    }

    @Override
    public void setVibrancy$sectionMeshConsumer(BlockFace.Consumer consumer) {
        vibrancy$sectionMeshConsumer = consumer;
    }
}
