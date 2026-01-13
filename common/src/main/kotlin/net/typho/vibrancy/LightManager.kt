package net.typho.vibrancy

import com.mojang.blaze3d.vertex.VertexBuffer
import net.minecraft.client.Camera
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.renderer.culling.Frustum
import net.minecraft.core.BlockPos
import net.minecraft.core.GlobalPos
import net.minecraft.core.SectionPos
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.chunk.LevelChunk
import net.minecraft.world.level.chunk.LevelChunkSection
import net.typho.big_shot_lib.BigShotLib
import net.typho.big_shot_lib.api.ITexture
import net.typho.big_shot_lib.api.impl.NeoShader
import net.typho.big_shot_lib.gl.GlStack
import net.typho.big_shot_lib.gl.state.GlCapability
import net.typho.vibrancy.block.BlockLight
import net.typho.vibrancy.block.BlockLightRegistry.get
import net.typho.vibrancy.block.BlockLightRegistry.has
import net.typho.vibrancy.block.BlockLightType
import net.typho.vibrancy.block.RenderingBlockLight
import net.typho.vibrancy.mixin.LevelRendererAccessor
import net.typho.vibrancy.shadows.BasicShadowMesher
import net.typho.vibrancy.shadows.ShadowGreedyMesher
import net.typho.vibrancy.shadows.ShadowMesher
import net.typho.vibrancy.shadows.entity.EntityShadowCollector
import net.typho.vibrancy.util.PointLight
import org.joml.Matrix4f
import java.util.*
import java.util.function.Consumer

open class LightManager {
    companion object {
        const val SHADOW_STENCIL_MASK: Int = 0b1
    }

    @JvmField
    protected var lightsRendered: Int = 0
    @JvmField
    protected var lightsRaytraced: Int = 0
    @JvmField
    protected var viewMatrix: Matrix4f? = null
    @JvmField
    val dirtyBlocks = LinkedList<GlobalPos>()
    @JvmField
    val blockLights = HashMap<BlockPos, BlockLight<*>>()
    @JvmField
    val entityShadows = EntityShadowCollector()

    fun getTickDelta(whilePaused: Boolean = true): Float = Minecraft.getInstance().timer.getGameTimeDeltaPartialTick(whilePaused)

    fun getLevel(): ClientLevel = Minecraft.getInstance().level!!

    fun clear() {
        blockLights.values.forEach { light -> light.free(this) }
        blockLights.clear()
    }

    fun getCamera(): Camera = Minecraft.getInstance().gameRenderer.mainCamera

    fun getViewMatrix() = Matrix4f(viewMatrix)

    fun getCullingFrustum(): Frustum = (Minecraft.getInstance().levelRenderer as LevelRendererAccessor).cullingFrustum

    fun createShadowMesher(light: PointLight): ShadowMesher? {
        return light.getShadowBox()?.let { box ->
            if (Vibrancy.config.forNerds.useGreedyMeshing) ShadowGreedyMesher(box) else BasicShadowMesher()
        }
    }

    fun rebuildAllShadows() {
        for (light in blockLights.values) {
            light.rebuildShadows(this)
        }
    }

    fun clearChunk(chunk: LevelChunk) {
        blockLights.entries.removeIf { entry ->
            val removed = ChunkPos(entry.key) == chunk.pos

            if (removed) {
                entry.value.free(this)
            }

            removed
        }
    }

    fun scanChunk(chunk: LevelChunk) {
        clearChunk(chunk)

        if (Vibrancy.config.blockLights.enabled) {
            for (i in chunk.minSection until chunk.maxSection) {
                val section = chunk.getSection(chunk.getSectionIndexFromSectionY(i))

                if (section.maybeHas { has(it.block) }) {
                    val minPos = SectionPos.of(chunk.pos, i).origin()

                    for (x in 0 until LevelChunkSection.SECTION_WIDTH) {
                        for (y in 0 until LevelChunkSection.SECTION_HEIGHT) {
                            for (z in 0 until LevelChunkSection.SECTION_WIDTH) {
                                val state = section.getBlockState(x, y, z)

                                get(state.block)?.addBlockLight(
                                    this,
                                    state,
                                    BlockPos(
                                        x + minPos.x,
                                        y + minPos.y,
                                        z + minPos.z
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <B : BlockLight<*>> render(type: BlockLightType<*, B>, lights: Set<RenderingBlockLight<*>>) {
        type.render(this, lights as Set<RenderingBlockLight<B>>)
    }

    fun render(camera: Camera = getCamera()) {
        viewMatrix = BigShotLib.getViewMatrix(camera)
        lightsRendered = 0
        lightsRaytraced = 0

        entityShadows.collect(this, blockLights.values)

        val byType = HashMap<BlockLightType<*, *>, MutableSet<RenderingBlockLight<*>>>()

        blockLights.values.stream()
            .sorted(Comparator.comparingDouble { light -> getSortingOrder(light) })
            .forEachOrdered { light ->
                val render = light.shouldRender(this) && shouldRender(light)
                var raytrace = false

                if (render) {
                    lightsRendered++

                    raytrace = light.shouldRaytrace(this) && shouldRaytrace(light)

                    if (raytrace) {
                        lightsRaytraced++
                    }
                }

                byType.computeIfAbsent(light.getType()) { HashSet() }.add(RenderingBlockLight(render, raytrace, light))
            }

        for (entry in byType) {
            render(entry.key, entry.value)
        }

        dirtyBlocks.clear()
    }

    fun blitOutput(output: ITexture) {
        GlStack().use { stack ->
            stack.disable(GlCapability.CULL_FACE)
            stack.disable(GlCapability.BLEND)

            val shader = NeoShader.get(Vibrancy.id("post"))!!
            shader.bind(stack)
            shader.setCommonUniforms()

            shader.setSampler("DiffuseSampler0", Minecraft.getInstance().mainRenderTarget.colorTextureId)
            shader.setSampler("VibrancyOutputSampler", output)
            shader.setSampler("VibrancyAlbedoSampler", VibrancyDynamicBuffers.albedoTexture!!)

            BigShotLib.SCREEN_VBO.bind()
            BigShotLib.SCREEN_VBO.draw()
            VertexBuffer.unbind()
        }
    }

    fun getDebugOutput(out: Consumer<String>) {
        out.accept("Block Lights")
        out.accept("${blockLights.size} lights in world")
        out.accept("${lightsRendered}/${Vibrancy.config.blockLights.maxRendered} rendered")
        out.accept("${lightsRaytraced}/${Vibrancy.config.blockLights.maxRaytraced} raytraced")

        val rendered = blockLights.values.stream()
            .filter { inRenderDistance(it) }
            .toList()

        out.accept("${rendered.sumOf { it.numShadows() }} shadows")
        out.accept("${rendered.sumOf { it.numAsyncTasksActive() }} async tasks")
        out.accept("${entityShadows.numEntities} entities")
        out.accept("${entityShadows.numBlockEntities} block entities")
    }

    fun inFrustum(light: BlockLight<*>): Boolean {
        return !Vibrancy.config.forNerds.useFrustumCulling || getCullingFrustum().isVisible(light.getBoundingBox())
    }

    protected fun cullDistanceBlocksSquared(): Int {
        val chunks = Vibrancy.config.blockLights.lightCullDistance.get()
        return chunks * chunks * 16 * 16
    }

    protected fun raytraceDistanceBlocksSquared(): Int {
        val chunks = Vibrancy.config.blockLights.raytraceDistance.get()
        return chunks * chunks * 16 * 16
    }

    fun getSortingOrder(light: BlockLight<*>): Double {
        return light.getBlockPos()?.distSqr(getCamera().blockPosition) ?: 0.0
    }

    fun inRenderDistance(light: BlockLight<*>): Boolean {
        return (light.getBlockPos()?.distSqr(getCamera().blockPosition) ?: 0.0) < cullDistanceBlocksSquared() && inFrustum(light)
    }

    fun inRaytraceDistance(light: BlockLight<*>): Boolean {
        return (light.getBlockPos()?.distSqr(getCamera().blockPosition) ?: 0.0) < raytraceDistanceBlocksSquared()
    }

    fun shouldRender(light: BlockLight<*>): Boolean {
        return lightsRendered < Vibrancy.config.blockLights.maxRendered && inRenderDistance(light)
    }

    fun shouldRaytrace(light: BlockLight<*>): Boolean {
        return lightsRaytraced < Vibrancy.config.blockLights.maxRaytraced && inRaytraceDistance(light)
    }
}