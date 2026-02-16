package net.typho.vibrancy

import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.ChatFormatting
import net.minecraft.client.Camera
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.BlockPos
import net.minecraft.core.GlobalPos
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.LevelChunk
import net.typho.big_shot_lib.api.client.rendering.buffers.AlbedoDynamicBuffer
import net.typho.big_shot_lib.api.client.rendering.buffers.NormalsDynamicBuffer
import net.typho.big_shot_lib.api.client.rendering.event.RenderData
import net.typho.big_shot_lib.api.client.rendering.shaders.NeoShaderRegistry
import net.typho.big_shot_lib.api.client.rendering.state.CullShard
import net.typho.big_shot_lib.api.client.rendering.state.DepthTestShard
import net.typho.big_shot_lib.api.client.rendering.state.GlFlag
import net.typho.big_shot_lib.api.client.rendering.state.RenderSettings
import net.typho.big_shot_lib.api.client.rendering.textures.GlFramebuffer
import net.typho.big_shot_lib.api.client.rendering.textures.GlTexture
import net.typho.big_shot_lib.api.client.rendering.util.MeshUtil
import net.typho.vibrancy.block.*
import net.typho.vibrancy.shadows.BasicShadowMesher
import net.typho.vibrancy.shadows.ShadowMesher
import net.typho.vibrancy.util.PointLight
import org.joml.Matrix4f
import org.joml.Vector2f
import org.joml.Vector4f
import java.util.*
import java.util.function.Consumer

open class LightManager {
    @JvmField
    val dirtyBlocks = LinkedList<GlobalPos>()
    @JvmField
    val blockLights = HashMap<BlockLightType<*, *, *>, BlockLightStorage<*>>()
    @JvmField
    protected var blockRenderResults = HashMap<BlockLightType<*, *, *>, LightRenderResult>()

    @JvmField
    val blitSettings = RenderSettings(
        Vibrancy.id("light_manager/blit"),
        listOf(
            CullShard.getDefault(),
            DepthTestShard.getDefault()
        )
    )

    fun getLevel(): ClientLevel = Minecraft.getInstance().level!!

    fun clear() {
        blockLights.values.forEach { storage -> storage.clear(this) }
    }

    fun getCamera(): Camera = Minecraft.getInstance().gameRenderer.mainCamera

    fun createShadowMesher(light: PointLight): ShadowMesher {
        return BasicShadowMesher()
    }

    fun rebuildAllShadows() {
        ensureStorageInitialized()

        for (light in blockLights.values) {
            light.rebuildShadows(this)
        }
    }

    fun ensureStorageInitialized() {
        Minecraft.getInstance().level?.let { level ->
            for (type in level.registryAccess().registryOrThrow(BlockLightRegistry.registryKey)) {
                blockLights.computeIfAbsent(type) { type -> type.createStorage(this) }
            }
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
    protected fun <S : BlockLightStorage<*>> castAndRender(data: RenderData, fbo: GlFramebuffer, type: BlockLightType<*, *, S>, storage: BlockLightStorage<*>): LightRenderResult {
        return type.render(this, data, storage as S, fbo)
    }

    fun render(data: RenderData, fbo: GlFramebuffer) {
        blockRenderResults.clear()

        for (entry in blockLights) {
            blockRenderResults[entry.key] = castAndRender(data, fbo, entry.key, entry.value)
        }

        dirtyBlocks.clear()
    }

    fun blitWorldPos(data: RenderData) {
        GlFlag.CULL_FACE.stack.push(false)

        val shader = NeoShaderRegistry.get(Vibrancy.id("world_pos"))!!
        shader.bind()
        shader.setCommonUniforms(data)

        shader.getUniform("DiffuseDepthSampler")?.setSampler(GlFramebuffer.MAIN.depthAttachment!! as GlTexture)

        shader.getUniform("IProjMat")?.setValue(Matrix4f(Vibrancy.iProjMat))
        shader.getUniform("IModelMat")?.setValue(Matrix4f(Vibrancy.iModelMat))

        shader.getUniform("CameraPos")?.setValue(Vibrancy.camera)

        MeshUtil.SCREEN_MESH.draw()

        shader.unbind()

        GlFlag.CULL_FACE.stack.pop()
    }

    fun blitOutput(data: RenderData, output: GlTexture) {
        blitSettings.bind()
        GlFlag.CULL_FACE.disable()
        GlFlag.DEPTH_TEST.disable()

        val shader = NeoShaderRegistry.get(Vibrancy.id("post"))!!
        shader.bind()
        shader.setCommonUniforms(data)

        shader.getUniform("DiffuseSampler0")?.setSampler(GlFramebuffer.MAIN.colorAttachments[0] as GlTexture)
        shader.getUniform("VibrancyWorldPosSampler")?.setSampler(Vibrancy.WORLD_POS_FBO.colorAttachments[0] as GlTexture)
        shader.getUniform("VibrancyOutputSampler")?.setSampler(output)
        shader.getUniform("VibrancyAlbedoSampler")?.setSampler(AlbedoDynamicBuffer.texture)
        shader.getUniform("VibrancyNormalSampler")?.setSampler(NormalsDynamicBuffer.texture)

        shader.getUniform("IProjMat")?.setValue(Matrix4f(Vibrancy.iProjMat))
        shader.getUniform("IModelMat")?.setValue(Matrix4f(Vibrancy.iModelMat))

        shader.getUniform("FogStart")?.setValue(RenderSystem.getShaderFogStart())
        shader.getUniform("FogEnd")?.setValue(RenderSystem.getShaderFogEnd())
        shader.getUniform("FogColor")?.setValue(Vector4f(RenderSystem.getShaderFogColor()))

        shader.getUniform("CameraPos")?.setValue(Vibrancy.camera)

        MeshUtil.SCREEN_MESH.draw()

        shader.unbind()
        blitSettings.unbind()
    }

    fun getDebugOutput(out: Consumer<String>) {
        out.accept("Block Lights")

        for (entry in blockLights) {
            out.accept("")

            out.accept(ChatFormatting.UNDERLINE.toString() + getLevel().registryAccess().registryOrThrow(BlockLightRegistry.registryKey).getKey(entry.key)!!.toString())
            out.accept("${entry.value.size()} lights in world")

            blockRenderResults[entry.key]?.accept(out)
        }
    }

    fun clampToRenderDistance(distance: Int): Int {
        return distance.coerceAtMost(Minecraft.getInstance().options.effectiveRenderDistance)
    }

    fun inRenderDistance(pos: BlockPos, distance: Int): Boolean {
        val d = clampToRenderDistance(distance)
        return pos.distSqr(getCamera().blockPosition) <= d * d * 16 * 16
    }

    fun inRenderDistance(pos: ChunkPos, distance: Int): Boolean {
        val centerChunk = Vector2f(pos.middleBlockX.toFloat(), pos.middleBlockZ.toFloat())
        val camera = getCamera().blockPosition.center.toVector3f()
        val d = clampToRenderDistance(distance)
        return centerChunk.distanceSquared(Vector2f(camera.x, camera.z)) <= d * d * 16 * 16
    }

    fun getSortingOrder(pos: BlockPos): Double {
        return pos.distSqr(getCamera().blockPosition)
    }

    fun getSortingOrder(pos: ChunkPos): Double {
        return pos.distanceSquared(ChunkPos(getCamera().blockPosition)).toDouble()
    }
}