package net.typho.vibrancy.block

import foundry.veil.api.client.color.Color
import foundry.veil.api.client.color.Colorc
import net.minecraft.core.BlockPos
import net.minecraft.core.SectionPos
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.LevelChunk
import net.minecraft.world.level.chunk.LevelChunkSection
import net.typho.vibrancy.api.DynamicLightInfo
import net.typho.vibrancy.api.PointLight
import net.typho.vibrancy.api.getKey
import net.typho.vibrancy.api.withBrightness
import org.joml.Vector3f

data class BlockLight(
    val blockPos: BlockPos,
    var offset: Vector3f,
    private var radius: Float,
    private var color: Colorc
) : PointLight() {
    constructor(blockPos: BlockPos, info: DynamicLightInfo, state: BlockState) : this(
        blockPos,
        info.offset.map{ it.apply(state) }
            .orElse(Vector3f(0.5f)),
        info.radius.map{ it.apply(state) }
            .orElse(15f),
        info.color.map{ it.apply(state) }
            .orElse(DEFAULT_COLOR)
            .withBrightness(info.brightness.map { it.apply(state) }.orElse(1f))
    )

    fun set(info: DynamicLightInfo, state: BlockState) {
        offset = info.offset.map{ it.apply(state) }
            .orElse(Vector3f(0.5f))
        radius = info.radius.map{ it.apply(state) }
            .orElse(15f)
        color = info.color.map{ it.apply(state) }
            .orElse(DEFAULT_COLOR)
            .withBrightness(info.brightness.map { it.apply(state) }.orElse(1f))
        dirty = true
    }

    override fun getPosition(): Vector3f = Vector3f(
        blockPos.x + offset.x,
        blockPos.y + offset.y,
        blockPos.z + offset.z
    )

    override fun getRadius(): Float = radius

    override fun getColor(): Colorc = color

    companion object {
        val LIGHTS = HashMap<BlockPos, BlockLight>()
        val DEFAULT_COLOR = Color(0xFFFF97)

        fun clearChunk(chunk: LevelChunk) {
            LIGHTS.entries.removeIf { entry ->
                val removed = ChunkPos(entry.key) == chunk.pos

                if (removed) {
                    entry.value.free()
                }

                removed
            }
        }

        fun scanChunk(chunk: LevelChunk) {
            clearChunk(chunk)

            for (i in chunk.minSection until chunk.maxSection) {
                val section = chunk.getSection(chunk.getSectionIndexFromSectionY(i))

                if (section.maybeHas { DynamicLightInfo.MAP.containsKey(it.block.getKey()) }) {
                    val minPos = SectionPos.of(chunk.pos, i).origin()

                    for (x in 0 until LevelChunkSection.SECTION_WIDTH) {
                        for (y in 0 until LevelChunkSection.SECTION_HEIGHT) {
                            for (z in 0 until LevelChunkSection.SECTION_WIDTH) {
                                val state = section.getBlockState(x, y, z)

                                DynamicLightInfo.MAP[state.block.getKey()]?.addBlockLight(
                                    BlockPos(
                                        x + minPos.x,
                                        y + minPos.y,
                                        z + minPos.z
                                    ), state
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}