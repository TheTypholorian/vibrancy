package net.typho.vibrancy.light

import net.minecraft.client.Camera
import net.minecraft.core.BlockPos
import net.minecraft.core.SectionPos
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.LevelChunk
import net.minecraft.world.level.chunk.LevelChunkSection
import net.typho.vibrancy.Vibrancy
import net.typho.vibrancy.util.getKey
import org.joml.Vector3f

data class BlockLight(
    private val blockPos: BlockPos,
    var offset: Vector3f,
    private var radius: Float,
    private var color: Vector3f
) : PointLight() {
    constructor(blockPos: BlockPos, info: BlockLightInfo, state: BlockState) : this(
        blockPos,
        info.offset.apply(state),
        info.radius.apply(state),
        Vector3f(info.color.apply(state)).mul(info.brightness.apply(state))
    )

    fun set(info: BlockLightInfo, state: BlockState) {
        offset = info.offset.apply(state)
        radius = info.radius.apply(state)
        color = Vector3f(info.color.apply(state)).mul(info.brightness.apply(state))
        boxDirty = true
        shadowsDirty = true
    }

    override fun getPosition(): Vector3f {
        return Vector3f(
            blockPos.x + offset.x,
            blockPos.y + offset.y,
            blockPos.z + offset.z
        )
    }

    override fun getBlockPos(): BlockPos = blockPos

    override fun getRadius(): Float = radius

    override fun getShadowRadius(manager: LightManager): Int =
        getRadius().coerceAtMost(Vibrancy.config.blockLights.shadowRadius.toFloat()).toInt()

    override fun getColor(): Vector3f = Vector3f(color).mul(Vibrancy.config.visuals.lightBrightness.get())

    override fun testCullingDistance(camera: Camera, chunks: Int): Boolean =
        getPosition().distanceSquared(camera.position.toVector3f()) <= (chunks * chunks * 256)

    companion object {
        @JvmField
        val LIGHTS = HashMap<BlockPos, BlockLight>()

        @JvmStatic
        fun clearChunk(chunk: LevelChunk) {
            LIGHTS.entries.removeIf { entry ->
                val removed = ChunkPos(entry.key) == chunk.pos

                if (removed) {
                    entry.value.free()
                }

                removed
            }
        }

        @JvmStatic
        fun scanChunk(chunk: LevelChunk) {
            clearChunk(chunk)

            for (i in chunk.minSection until chunk.maxSection) {
                val section = chunk.getSection(chunk.getSectionIndexFromSectionY(i))

                if (section.maybeHas { BlockLightInfo.MAP.containsKey(it.block.getKey()) }) {
                    val minPos = SectionPos.of(chunk.pos, i).origin()

                    for (x in 0 until LevelChunkSection.SECTION_WIDTH) {
                        for (y in 0 until LevelChunkSection.SECTION_HEIGHT) {
                            for (z in 0 until LevelChunkSection.SECTION_WIDTH) {
                                val state = section.getBlockState(x, y, z)

                                BlockLightInfo.get(state.block)?.addBlockLight(
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