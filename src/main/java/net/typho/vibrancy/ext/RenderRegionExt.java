package net.typho.vibrancy.ext;

import net.caffeinemc.mods.sodium.client.gl.device.MultiDrawBatch;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import net.typho.vibrancy.mirror.VibrancySectionRenderDataStorage;

public interface RenderRegionExt {
    VibrancySectionRenderDataStorage vibrancy$getOverlayStorage(TerrainRenderPass pass);

    VibrancySectionRenderDataStorage vibrancy$getOrCreateOverlayStorage(TerrainRenderPass pass);

    MultiDrawBatch vibrancy$getCachedOverlayBatch(TerrainRenderPass pass);
}
