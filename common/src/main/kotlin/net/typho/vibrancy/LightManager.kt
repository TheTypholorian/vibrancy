package net.typho.vibrancy

import com.mojang.blaze3d.vertex.VertexBuffer
import net.minecraft.client.Camera
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.renderer.culling.Frustum
import net.minecraft.core.GlobalPos
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.chunk.LevelChunk
import net.typho.big_shot_lib.BigShotLib
import net.typho.big_shot_lib.api.ITexture
import net.typho.big_shot_lib.api.impl.NeoShader
import net.typho.big_shot_lib.gl.GlStack
import net.typho.big_shot_lib.gl.state.GlCapability
import net.typho.vibrancy.block.BlockLight
import net.typho.vibrancy.block.BlockLightStorage
import net.typho.vibrancy.block.BlockLightType
import net.typho.vibrancy.block.BlockRenderResult
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
    protected var renderResult = BlockRenderResult()
    @JvmField
    protected var viewMatrix: Matrix4f? = null
    @JvmField
    val dirtyBlocks = LinkedList<GlobalPos>()
    @JvmField
    val blockLights = HashMap<BlockLightType<*, *, *>, BlockLightStorage<*>>()
    @JvmField
    val entityShadows = EntityShadowCollector()

    fun getTickDelta(whilePaused: Boolean = true): Float =
        Minecraft.getInstance().timer.getGameTimeDeltaPartialTick(whilePaused)

    fun getLevel(): ClientLevel = Minecraft.getInstance().level!!

    fun clear() {
        blockLights.values.forEach { storage -> storage.clear(this) }
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

    fun loadChunk(chunk: LevelChunk) {
        blockLights.values.forEach { storage -> storage.loadChunk(this, chunk) }
    }

    fun deloadChunk(chunk: LevelChunk) {
        blockLights.values.forEach { storage -> storage.deloadChunk(this, chunk) }
    }

    @Suppress("UNCHECKED_CAST")
    protected fun <S : BlockLightStorage<*>> castAndRender(type: BlockLightType<*, *, S>, storage: BlockLightStorage<*>): BlockRenderResult {
        return type.render(this, storage as S)
    }

    fun render(camera: Camera = getCamera()) {
        viewMatrix = BigShotLib.getViewMatrix(camera)
        renderResult = BlockRenderResult()

        // TODO fix entity shadows
        //entityShadows.collect(this, blockLights.values)

        for (entry in blockLights) {
            renderResult.add(castAndRender(entry.key, entry.value))
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
        out.accept("${renderResult.numRendered} rendered")
        out.accept("${renderResult.numRaytraced} raytraced")
        out.accept("${renderResult.numShadows} shadows")
        out.accept("${renderResult.numAsyncTasks} async tasks")

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

    fun inRenderDistance(light: BlockLight<*>): Boolean {
        return (light.getBlockPos()?.distSqr(getCamera().blockPosition) ?: 0.0) < cullDistanceBlocksSquared()
                && inFrustum(light)
    }

    fun inRenderDistance(chunk: ChunkPos): Boolean {
        return chunk.getMiddleBlockPosition(getCamera().blockPosition.y).distSqr(getCamera().blockPosition) < cullDistanceBlocksSquared()
    }

    fun inRaytraceDistance(light: BlockLight<*>): Boolean {
        return (light.getBlockPos()?.distSqr(getCamera().blockPosition) ?: 0.0) < raytraceDistanceBlocksSquared()
    }

    fun inRaytraceDistance(chunk: ChunkPos): Boolean {
        return chunk.getMiddleBlockPosition(getCamera().blockPosition.y).distSqr(getCamera().blockPosition) < raytraceDistanceBlocksSquared()
    }
}