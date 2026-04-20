package net.typho.vibrancy.sky.impl

import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.chunk.LevelChunk
import net.typho.big_shot_lib.api.client.rendering.opengl.constant.GlClearBit
import net.typho.big_shot_lib.api.math.rect.NeoRect2i
import net.typho.big_shot_lib.api.util.NeoColor
import net.typho.vibrancy.LightManager
import net.typho.vibrancy.shadows.LightTexture
import net.typho.vibrancy.shadows.SkyLightMeshManager
import net.typho.vibrancy.sky.ChunkedSkyLightStorage
import net.typho.vibrancy.sky.SkyLightStorage

class OverworldSkyLightStorage : ChunkedSkyLightStorage<OverworldSkyLightInfo, OverworldSkyLightStorage.Chunk>(OverworldSkyLightType) {
    override fun createChunk(
        manager: LightManager,
        level: Level,
        pos: ChunkPos
    ) = Chunk(pos)

    override fun load(
        manager: LightManager,
        info: OverworldSkyLightInfo
    ) {
        TODO("Not yet implemented")
    }

    class Chunk(
        @JvmField
        val pos: ChunkPos
    ) : SkyLightStorage<OverworldSkyLightInfo> {
        @JvmField
        val texture = LightTexture().also { it.resize(256, 256) }
        @JvmField
        val mesh = SkyLightMeshManager { mesh ->
            texture.framebuffer.bind(NeoRect2i(0, 0, texture.width, texture.height)).use { fbo ->
                fbo.clear(GlClearBit.Color(NeoColor.FULL_ON))
                // TODO draw
            }
        }

        override fun load(
            manager: LightManager,
            info: OverworldSkyLightInfo
        ) {
            TODO("Not yet implemented")
        }

        override fun reload(manager: LightManager) {
            TODO("Not yet implemented")
        }

        override fun loadChunk(
            manager: LightManager,
            chunk: LevelChunk
        ) {
            TODO("Not yet implemented")
        }

        override fun deloadChunk(
            manager: LightManager,
            chunk: LevelChunk
        ) {
            TODO("Not yet implemented")
        }

        override fun clear(manager: LightManager) {
            TODO("Not yet implemented")
        }
    }
}