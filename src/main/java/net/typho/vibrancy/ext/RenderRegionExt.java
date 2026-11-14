package net.typho.vibrancy.ext;

import net.caffeinemc.mods.sodium.client.gl.device.MultiDrawBatch;
import net.caffeinemc.mods.sodium.client.render.chunk.data.SectionRenderDataStorage;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;

public interface RenderRegionExt {
    SectionRenderDataStorage vibrancy$getOverlayStorage(TerrainRenderPass pass);

    SectionRenderDataStorage vibrancy$getOrCreateOverlayStorage(TerrainRenderPass pass);

    MultiDrawBatch vibrancy$getCachedOverlayBatch(TerrainRenderPass pass);
}
