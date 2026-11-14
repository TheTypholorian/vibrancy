package net.typho.vibrancy.mirror;

import net.caffeinemc.mods.sodium.client.gl.arena.PendingUpload;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSection;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;

public record PendingSectionOverlayIndexBufferUpload(RenderSection section, TerrainRenderPass pass, PendingUpload indexBufferUpload) {
}
