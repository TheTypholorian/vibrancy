package net.typho.vibrancy.light

import foundry.veil.api.client.color.Color
import foundry.veil.api.client.color.Colorc
import net.minecraft.client.Camera
import net.minecraft.core.BlockPos
import net.minecraft.core.SectionPos
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.LevelChunk
import net.minecraft.world.level.chunk.LevelChunkSection
import net.typho.vibrancy.Vibrancy
import net.typho.vibrancy.util.getKey
import net.typho.vibrancy.util.withBrightness
import org.joml.Vector3f
import java.util.function.Supplier

data class BlockLight(
    private val blockPos: BlockPos,
    var offset: Supplier<Vector3f>,
    private var radius: Supplier<Float>,
    private var color: Supplier<Colorc>
) : PointLight() {
    constructor(blockPos: BlockPos, info: DynamicLightInfo, state: BlockState) : this(
        blockPos,
        {
            info.offset.map { it.apply(state) }
                .orElse(Vector3f(0.5f))
        },
        {
            info.radius.map { it.apply(state) }
                .orElse(15f)
        },
        {
            info.color.map { it.apply(state) }
                .orElse(DEFAULT_COLOR)
                .withBrightness(info.brightness.map { it.apply(state) }.orElse(1f) * Vibrancy.LIGHT_BRIGHTNESS)
        }
    )

    fun set(info: DynamicLightInfo, state: BlockState) {
        offset = Supplier {
            info.offset.map { it.apply(state) }
                .orElse(Vector3f(0.5f))
        }
        radius = Supplier {
            info.radius.map { it.apply(state) }
                .orElse(15f)
        }
        color = Supplier {
            info.color.map { it.apply(state) }
                .orElse(DEFAULT_COLOR)
                .withBrightness(info.brightness.map { it.apply(state) }.orElse(1f) * Vibrancy.LIGHT_BRIGHTNESS)
        }
        boxDirty = true
        shadowsDirty = true
    }

    override fun getPosition(): Vector3f {
        val offset = this.offset.get()
        return Vector3f(
            blockPos.x + offset.x,
            blockPos.y + offset.y,
            blockPos.z + offset.z
        )
    }

    override fun getBlockPos(): BlockPos = blockPos

    override fun getRadius(): Float = radius.get()

    override fun getShadowRadius(manager: LightManager): Int =
        getRadius().coerceAtMost(manager.shadowRadius.toFloat()).toInt()

    override fun getColor(): Colorc = color.get()

    override fun testCullingDistance(camera: Camera, chunks: Int): Boolean =
        getPosition().distanceSquared(camera.position.toVector3f()) <= (chunks * chunks * 256)

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