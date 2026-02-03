package net.typho.vibrancy.sky.impl

import com.mojang.blaze3d.vertex.*
import net.minecraft.client.renderer.RenderType
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.LightLayer
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.LevelChunk
import net.typho.vibrancy.LightManager
import net.typho.vibrancy.shadows.ShadowMesher
import net.typho.vibrancy.shadows.ShadowPredicate
import net.typho.vibrancy.sky.SkyLight
import org.joml.Vector3f
import org.lwjgl.system.NativeResource

class OverworldSkyLight(
    @JvmField
    val info: OverworldSkyLightInfo
) : SkyLight<OverworldSkyLightInfo, OverworldSkyLight> {
    @JvmField
    val sections: MutableMap<ChunkPos, Section> = HashMap()

    override fun type() = OverworldSkyLightType

    override fun rebuildShadows(manager: LightManager) {
        sections.values.forEach { it.dirty = true }
    }

    override fun loadChunk(
        manager: LightManager,
        chunk: LevelChunk
    ) {
        sections.computeIfAbsent(chunk.pos, ::Section).dirty = true
    }

    override fun deloadChunk(
        manager: LightManager,
        chunk: LevelChunk
    ) {
        sections.remove(chunk.pos)?.free()
    }

    override fun clear(manager: LightManager) {
        sections.values.forEach(Section::free)
        sections.clear()
    }

    class Section(
        @JvmField
        val pos: ChunkPos
    ) : NativeResource {
        @JvmField
        val vbo = VertexBuffer(VertexBuffer.Usage.STATIC)
        @JvmField
        var dirty = true
        @JvmField
        var any = false

        override fun free() {
            vbo.close()
        }

        fun reload(
            manager: LightManager,
            chunk: LevelChunk
        ) {
            val blocks = HashMap<BlockPos, HashSet<Direction>>()

            for (x in 0 until 16) {
                for (z in 0 until 16) {
                    for (y in chunk.maxBuildHeight - 1 downTo chunk.minBuildHeight) {
                        val pos = BlockPos(x, y, z)

                        blocks.computeIfAbsent(pos) { HashSet() }.add(Direction.UP)

                        if (chunk.level.getBrightness(LightLayer.SKY, pos) == 0) {
                            break
                        }

                        for (dir in Direction.entries) {
                            if (dir.axis != Direction.Axis.Y) {
                                blocks.computeIfAbsent(pos.relative(dir)) { HashSet() }.add(dir.opposite)
                            }
                        }
                    }
                }
            }

            fun isInChunk(pos: BlockPos): Boolean {
                if (chunk.isOutsideBuildHeight(pos)) {
                    return false
                }

                return pos.x >= chunk.pos.minBlockX && pos.x <= chunk.pos.maxBlockX && pos.z >= chunk.pos.minBlockZ && pos.z <= chunk.pos.maxBlockZ
            }

            blocks.keys.removeIf { pos -> !isInChunk(pos) }

            val buffer = ByteBufferBuilder(RenderType.TRANSIENT_BUFFER_SIZE)
            val builder = BufferBuilder(buffer, VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX)
            any = false

            for (entry in blocks) {
                ShadowMesher.collectLightFaces(
                    manager,
                    chunk.getBlockState(entry.key),
                    chunk.level,
                    entry.key,
                    object : ShadowPredicate {
                        override fun shouldCastBlock(
                            state: BlockState,
                            level: Level,
                            pos: BlockPos
                        ) = blocks.contains(pos)

                        override fun shouldCastFace(
                            face: Direction?,
                            state: BlockState,
                            level: Level,
                            pos: BlockPos
                        ): Boolean = face == null || blocks[pos]?.contains(face) == true

                        override fun isInRange(pos: BlockPos) = isInChunk(pos)
                    }
                ) { quad ->
                    quad.buildGeometry(builder, Vector3f(pos.x * 16f, 0f, pos.z * 16f))
                    any = true
                }
            }

            if (any) {
                vbo.bind()
                vbo.upload(builder.buildOrThrow())
                VertexBuffer.unbind()
            }
        }
    }
}