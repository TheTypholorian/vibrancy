package net.typho.vibrancy

import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.VertexBuffer
import net.minecraft.client.Camera
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.renderer.culling.Frustum
import net.minecraft.core.BlockPos
import net.minecraft.core.GlobalPos
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.LevelChunk
import net.minecraft.world.phys.AABB
import net.typho.big_shot_lib.BigShotLib
import net.typho.big_shot_lib.api.ITexture
import net.typho.big_shot_lib.api.impl.NeoShader
import net.typho.big_shot_lib.gl.GlStack
import net.typho.big_shot_lib.gl.state.DepthMask
import net.typho.big_shot_lib.gl.state.GlCapability
import net.typho.vibrancy.block.*
import net.typho.vibrancy.mixin.LevelRendererAccessor
import net.typho.vibrancy.shadows.BasicShadowMesher
import net.typho.vibrancy.shadows.ShadowGreedyMesher
import net.typho.vibrancy.shadows.ShadowMesher
import net.typho.vibrancy.shadows.entity.EntityShadowCollector
import net.typho.vibrancy.util.PointLight
import org.joml.Matrix4f
import org.joml.Vector2f
import org.joml.Vector4f
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
    @JvmField
    var debugMode = false

    fun getTickDelta(whilePaused: Boolean = true): Float =
        Minecraft.getInstance().timer.getGameTimeDeltaPartialTick(whilePaused)

    fun getLevel(): ClientLevel = Minecraft.getInstance().level!!

    fun clear() {
        blockLights.values.forEach { storage -> storage.clear(this) }
    }

    fun getCamera(): Camera = Minecraft.getInstance().gameRenderer.mainCamera

    fun getViewMatrix() = Matrix4f(viewMatrix)

    fun getCullingFrustum(): Frustum = (Minecraft.getInstance().levelRenderer as LevelRendererAccessor).cullingFrustum

    fun createShadowMesher(light: PointLight): ShadowMesher? {
        return light.getShadowBox()?.let { box ->
            if (Vibrancy.config.forNerds.useGreedyMeshing.get()) ShadowGreedyMesher(box) else BasicShadowMesher()
        }
    }

    fun rebuildAllShadows() {
        ensureStorageInitialized()

        for (light in blockLights.values) {
            light.rebuildShadows(this)
        }
    }

    fun ensureStorageInitialized() {
        for (type in BlockLightRegistry.types) {
            blockLights.computeIfAbsent(type) { type -> type.createStorage() }
        }
    }

    @Suppress("UNCHECKED_CAST")
    protected fun <I : BlockLightInfo<I, B>, B : BlockLight<I, B>> addBlockLight(
        pos: BlockPos,
        state: BlockState,
        light: BlockLightInfo<I, B>
    ) {
        (blockLights[light.type()] as BlockLightStorage<I>).addLight(this, state, pos, light as I)
    }

    fun blockChanged(
        level: Level,
        pos: BlockPos,
        old: BlockState,
        new: BlockState
    ) {
        ensureStorageInitialized()

        val oldLight = BlockLightRegistry.get(old.block)
        val newLight = BlockLightRegistry.get(new.block)

        if (oldLight != null) {
            blockLights[oldLight.type()]!!.removeLight(this, pos)
        }

        if (newLight != null) {
            addBlockLight(pos, new, newLight)
        }

        dirtyBlocks.add(GlobalPos(level.dimension(), pos))
    }

    fun loadChunk(chunk: LevelChunk) {
        ensureStorageInitialized()

        blockLights.values.forEach { storage -> storage.loadChunk(this, chunk) }
    }

    fun deloadChunk(chunk: LevelChunk) {
        ensureStorageInitialized()

        blockLights.values.forEach { storage -> storage.deloadChunk(this, chunk) }
    }

    @Suppress("UNCHECKED_CAST")
    protected fun <S : BlockLightStorage<*>> castAndRender(type: BlockLightType<*, *, S>, storage: BlockLightStorage<*>): BlockRenderResult {
        return type.render(this, storage as S)
    }

    @Suppress("UNCHECKED_CAST")
    protected fun <S : BlockLightStorage<*>> castAndRenderDebug(type: BlockLightType<*, *, S>, storage: BlockLightStorage<*>) {
        type.renderDebug(this, storage as S)
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

    fun renderDebug() {
        if (debugMode) {
            for (entry in blockLights) {
                castAndRenderDebug(entry.key, entry.value)
            }
        }
    }

    fun blitWorldPos() {
        GlStack().use { stack ->
            stack.disable(GlCapability.CULL_FACE)
            stack.disable(GlCapability.BLEND)

            val shader = NeoShader.get(Vibrancy.id("world_pos"))!!
            shader.bind(stack)
            shader.setCommonUniforms()

            shader.setSampler("DiffuseDepthSampler", Minecraft.getInstance().mainRenderTarget.depthTextureId)

            shader.getUniform("IProjMat")?.set(Matrix4f(Vibrancy.iProjMat))
            shader.getUniform("IModelMat")?.set(Matrix4f(Vibrancy.iModelMat))

            shader.getUniform("CameraPos")?.set(Vibrancy.camera)

            BigShotLib.SCREEN_VBO.bind()
            BigShotLib.SCREEN_VBO.draw()
            VertexBuffer.unbind()
        }
    }

    fun blitOutput(output: ITexture) {
        GlStack().use { stack ->
            stack.disable(GlCapability.CULL_FACE)
            stack.disable(GlCapability.BLEND)
            stack.disable(GlCapability.DEPTH_TEST)
            stack.set(DepthMask, false)

            val shader = NeoShader.get(Vibrancy.id("post"))!!
            shader.bind(stack)
            shader.setCommonUniforms()

            shader.setSampler("DiffuseSampler0", Minecraft.getInstance().mainRenderTarget.colorTextureId)
            shader.setSampler("VibrancyWorldPosSampler", Vibrancy.WORLD_POS_FBO.colorAttachments[0] as ITexture)
            shader.setSampler("VibrancyOutputSampler", output)
            shader.setSampler("VibrancyAlbedoSampler", VibrancyDynamicBuffers.albedoTexture!!)

            shader.getUniform("IProjMat")?.set(Matrix4f(Vibrancy.iProjMat))
            shader.getUniform("IModelMat")?.set(Matrix4f(Vibrancy.iModelMat))

            shader.getUniform("FogStart")?.set(RenderSystem.getShaderFogStart())
            shader.getUniform("FogEnd")?.set(RenderSystem.getShaderFogEnd())
            shader.getUniform("FogColor")?.set(Vector4f(RenderSystem.getShaderFogColor()))

            shader.getUniform("CameraPos")?.set(Vibrancy.camera)

            BigShotLib.SCREEN_VBO.bind()
            BigShotLib.SCREEN_VBO.draw()
            VertexBuffer.unbind()
        }
    }

    fun getDebugOutput(out: Consumer<String>) {
        out.accept("Block Lights")
        out.accept("${blockLights.values.sumOf { storage -> storage.size() }} lights in world")
        out.accept("${renderResult.numRendered} rendered")
        out.accept("${renderResult.numRaytraced} raytraced")
        out.accept("${renderResult.numShadows} shadows")
        out.accept("${renderResult.numAsyncTasks} async tasks")

        out.accept("${entityShadows.numEntities} entities")
        out.accept("${entityShadows.numBlockEntities} block entities")
    }

    fun inFrustum(box: AABB): Boolean {
        return !Vibrancy.config.forNerds.useFrustumCulling || getCullingFrustum().isVisible(box)
    }

    fun inRenderDistance(pos: BlockPos, distance: Int): Boolean {
        return pos.distSqr(getCamera().blockPosition) <= distance * distance * 16 * 16
    }

    fun inRenderDistance(pos: ChunkPos, distance: Int): Boolean {
        val centerChunk = Vector2f(pos.middleBlockX.toFloat(), pos.middleBlockZ.toFloat())
        val camera = getCamera().blockPosition.center.toVector3f()
        return centerChunk.distanceSquared(Vector2f(camera.x, camera.z)) <= distance * distance * 16 * 16
    }

    fun getSortingOrder(pos: BlockPos): Double {
        return pos.distSqr(getCamera().blockPosition)
    }
}