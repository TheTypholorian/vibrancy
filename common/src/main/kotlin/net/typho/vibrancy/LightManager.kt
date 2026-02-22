package net.typho.vibrancy

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
import net.typho.big_shot_lib.api.client.registration.events.RenderEventData
import net.typho.big_shot_lib.api.client.rendering.buffers.AlbedoDynamicBuffer
import net.typho.big_shot_lib.api.client.rendering.buffers.NormalsDynamicBuffer
import net.typho.big_shot_lib.api.client.rendering.shaders.NeoShaderRegistry
import net.typho.big_shot_lib.api.client.rendering.state.DisableFlagsShard
import net.typho.big_shot_lib.api.client.rendering.state.FogUtil
import net.typho.big_shot_lib.api.client.rendering.state.GlFlag
import net.typho.big_shot_lib.api.client.rendering.state.RenderSettings
import net.typho.big_shot_lib.api.client.rendering.textures.GlFramebuffer
import net.typho.big_shot_lib.api.client.rendering.textures.GlTexture
import net.typho.big_shot_lib.api.client.rendering.util.MeshUtil
import net.typho.big_shot_lib.api.services.WrapperUtil
import net.typho.vibrancy.block.*
import net.typho.vibrancy.shadows.BasicShadowMesher
import net.typho.vibrancy.shadows.ShadowMesher
import net.typho.vibrancy.util.PointLight
import org.joml.Matrix4f
import org.joml.Vector2f
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
            DisableFlagsShard(listOf(
                GlFlag.CULL_FACE,
                GlFlag.DEPTH_TEST,
                GlFlag.BLEND
            ))
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
        for (light in blockLights.values) {
            light.rebuildShadows(this)
        }
    }

    fun resizeAllShadows() {
        for (light in blockLights.values) {
            light.resizeShadows(this)
        }

        rebuildAllShadows()
    }

    fun ensureStorageInitialized() {
        Minecraft.getInstance().level?.let { level ->
            for (type in WrapperUtil.INSTANCE.wrap(level.registryAccess()).registry(BlockLightRegistry.registryKey)!!.values()) {
                blockLights.computeIfAbsent(type) { type -> type.createStorage(this) }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    protected fun <I : BlockLightInfo<I, B>, B : BlockLight<I, B>> addBlockLight(
        level: Level,
        pos: BlockPos,
        state: BlockState,
        light: BlockLightInfo<I, B>
    ) {
        (blockLights[light.type()] as BlockLightStorage<I>).addLight(this, level, state, pos, light as I)
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
            addBlockLight(level, pos, new, newLight)
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
    protected fun <S : BlockLightStorage<*>> castAndRender(data: RenderEventData, fbo: GlFramebuffer, type: BlockLightType<*, *, S>, storage: BlockLightStorage<*>): LightRenderResult {
        return type.render(this, data, storage as S, fbo)
    }

    fun render(data: RenderEventData, fbo: GlFramebuffer) {
        blockRenderResults.clear()

        for (entry in blockLights) {
            blockRenderResults[entry.key] = castAndRender(data, fbo, entry.key, entry.value)
        }

        dirtyBlocks.clear()
    }

    fun blitWorldPos(data: RenderEventData) {
        GlFlag.CULL_FACE.stack.push(false)

        val shader = NeoShaderRegistry.get(Vibrancy.id("world_pos"))!!
        shader.bind()
        shader.setCommonUniforms(data)

        shader.getUniform("DiffuseDepthSampler")?.setSampler(GlFramebuffer.MAIN.depthAttachment!! as GlTexture)

        shader.getUniform("IProjMat")?.setValue(Matrix4f(data.inverseProjMat))
        shader.getUniform("IModelMat")?.setValue(Matrix4f(data.inverseModelViewMat))

        shader.getUniform("CameraPos")?.setValue(data.camera.position.toVector3f())

        MeshUtil.SCREEN_MESH.draw()

        shader.unbind()

        GlFlag.CULL_FACE.stack.pop()
    }

    fun blitOutput(data: RenderEventData, output: GlTexture) {
        GlFlag.BLEND.disable()
        blitSettings.bind()

        val shader = NeoShaderRegistry.get(Vibrancy.id("post"))!!
        shader.bind()
        shader.setCommonUniforms(data)

        shader.getUniform("DiffuseSampler0")?.setSampler(GlFramebuffer.MAIN.colorAttachments[0] as GlTexture)
        shader.getUniform("VibrancyWorldPosSampler")?.setSampler(Vibrancy.worldPosFbo.colorAttachments[0] as GlTexture)
        shader.getUniform("VibrancyOutputSampler")?.setSampler(output)
        shader.getUniform("VibrancyAlbedoSampler")?.setSampler(AlbedoDynamicBuffer.texture)
        shader.getUniform("VibrancyNormalSampler")?.setSampler(NormalsDynamicBuffer.texture)

        shader.getUniform("IProjMat")?.setValue(Matrix4f(data.inverseProjMat))
        shader.getUniform("IModelMat")?.setValue(Matrix4f(data.inverseModelViewMat))

        shader.getUniform("CameraPos")?.setValue(data.camera.position.toVector3f())

        FogUtil.INSTANCE.upload(shader)

        MeshUtil.SCREEN_MESH.draw()

        shader.unbind()
        blitSettings.unbind()
    }

    fun getDebugOutput(out: Consumer<String>) {
        for (entry in blockLights) {
            out.accept(ChatFormatting.UNDERLINE.toString() + WrapperUtil.INSTANCE.wrap(getLevel().registryAccess()).registry(BlockLightRegistry.registryKey)!!.getKey(entry.key).location.toString())
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