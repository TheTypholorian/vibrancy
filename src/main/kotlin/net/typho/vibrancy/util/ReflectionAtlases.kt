package net.typho.vibrancy.util

import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.texture.SpriteContents
import net.minecraft.client.renderer.texture.atlas.SpriteResourceLoader
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection
import net.minecraft.resources.FileToIdConverter
import net.minecraft.resources.Identifier
import net.minecraft.server.packs.resources.ResourceManager
import net.typho.big_shot_lib.api.client.event.ClientStartTickEvent
import net.typho.big_shot_lib.api.client.rendering.common.GpuObjects
import net.typho.big_shot_lib.api.client.rendering.common.GpuQueue
import net.typho.big_shot_lib.api.client.rendering.common.GpuTexture
import net.typho.big_shot_lib.api.client.rendering.common.constant.GpuTextureUsage
import net.typho.big_shot_lib.api.event.NeoClientEventBus
import net.typho.big_shot_lib.api.util.resource.NamedResource
import net.typho.big_shot_lib.api.util.resource.SingleStepNeoReloadListener
import net.typho.vibrancy.Vibrancy
import net.typho.vibrancy.mixin.SpriteContentsAccessor
import java.io.FileNotFoundException

object ReflectionAtlases : NamedResource, SingleStepNeoReloadListener {
    private class Animation(
        @JvmField
        val x: Int,
        @JvmField
        val y: Int,
        @JvmField
        val contents: SpriteContents,
        //? if <1.21.11 {
        /*@JvmField
        val ticker: SpriteTicker
        *///? }
    )

    private class Atlas(
        @JvmField
        val texture: GpuTexture,
        @JvmField
        val animations: List<Animation>
    )

    @JvmField
    val idConverter = FileToIdConverter("rtx/reflections", "png")
    override val location: Identifier = Vibrancy.id("reflection_atlases")
    private val atlases = hashMapOf<Identifier, Atlas>()

    override fun onResourceManagerReload(manager: ResourceManager) {
        atlases.forEach { (key, atlas) -> GpuQueue.runOrQueue {
            atlas.texture.recycle()

            for (animation in atlas.animations) {
                animation.contents.close()
            }
        } }
        atlases.clear()
    }

    // TODO
    fun onInitializeClient(bus: NeoClientEventBus) {
        bus.register(ClientStartTickEvent {
            for ((key, atlas) in atlases) {
                /*
                atlas.texture.bind(GlTextureTarget.TEXTURE_2D).use { texture ->
                    for (animation in atlas.animations) {
                        //? if <1.21.5 {
                        /*animation.ticker.tickAndUpload(animation.x, animation.y)
                        *///? } else if <1.21.11 {
                        /*animation.ticker.tickAndUpload(animation.x, animation.y, GlTextureAccessor.`vibrancy$init`(
                            //? if >=1.21.6 {
                            GpuTexture.USAGE_COPY_DST,
                            //? }
                            "Vibrancy Reflection Atlas $key",
                            TextureFormat.RGBA8,
                            atlas.texture.width!!,
                            atlas.texture.height!!,
                            //? if >=1.21.6 {
                            1,
                            //? }
                            1,
                            atlas.texture.glId
                        ))
                        *///? }
                    }
                }
                 */
            }
        })
    }

    operator fun get(key: Identifier, resources: ResourceManager = Minecraft.getInstance().resourceManager): GpuTexture {
        return atlases.computeIfAbsent(key) { key ->
            val parent = try {
                Minecraft.getInstance().atlasManager.getAtlasOrThrow(key)
            } catch (_: NullPointerException) {
                throw FileNotFoundException("No atlas $key")
            }
            val texture = GpuObjects.texture({ "Vibrancy Reflection Atlas $key" }, parent.texture.width, parent.texture.height, GpuTextureUsage.COPY_DST or GpuTextureUsage.COPY_SRC or GpuTextureUsage.TEXTURE_BINDING or GpuTextureUsage.RENDER_ATTACHMENT)
            val loader = SpriteResourceLoader.create(setOf(AnimationMetadataSection.TYPE))

            for (resource in idConverter.listMatchingResources(resources)) {
                val id = idConverter.fileToId(resource.key)
                val sprite = parent.getSprite(id)

                resource.value.open().use { stream ->
                    loader.loadSprite(resource.key, resource.value)?.let { contents ->
                        texture.upload(
                            (contents as SpriteContentsAccessor).`vibrancy$getOriginalImage`(),
                            0,
                            0,
                            sprite.x,
                            sprite.y
                        )
                    }
                }
            }

            return@computeIfAbsent Atlas(texture, listOf())
        }.texture
    }
}