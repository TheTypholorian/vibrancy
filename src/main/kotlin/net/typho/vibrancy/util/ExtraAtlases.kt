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

object ExtraAtlases : NamedResource, SingleStepNeoReloadListener {
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
    val reflectionIdConverter = FileToIdConverter("rtx/reflections", "png")
    @JvmField
    val transmissionIdConverter = FileToIdConverter("rtx/transmission", "png")
    override val location: Identifier = Vibrancy.id("extra_atlases")
    private val reflection = hashMapOf<Identifier, Atlas>()
    private val transmission = hashMapOf<Identifier, Atlas>()

    override fun onResourceManagerReload(manager: ResourceManager) {
        reflection.forEach { (key, atlas) -> GpuQueue.runOrQueue {
            atlas.texture.recycle()

            for (animation in atlas.animations) {
                animation.contents.close()
            }
        } }
        reflection.clear()

        transmission.forEach { (key, atlas) -> GpuQueue.runOrQueue {
            atlas.texture.recycle()

            for (animation in atlas.animations) {
                animation.contents.close()
            }
        } }
        transmission.clear()
    }

    // TODO
    @JvmStatic
    fun onInitializeClient(bus: NeoClientEventBus) {
        bus.register(ClientStartTickEvent {
            for ((key, atlas) in reflection) {
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

    private fun createAtlas(type: String, key: Identifier, idConverter: FileToIdConverter, resources: ResourceManager): Atlas {
        val parent = try {
            Minecraft.getInstance().atlasManager.getAtlasOrThrow(key)
        } catch (_: NullPointerException) {
            throw FileNotFoundException("No atlas $key")
        }
        val texture = GpuObjects.texture({ "Vibrancy $type Atlas $key" }, parent.texture.width, parent.texture.height, GpuTextureUsage.COPY_DST or GpuTextureUsage.COPY_SRC or GpuTextureUsage.TEXTURE_BINDING)
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

        return Atlas(texture, listOf())
    }

    @JvmStatic
    fun getReflection(key: Identifier, resources: ResourceManager = Minecraft.getInstance().resourceManager): GpuTexture {
        return reflection.computeIfAbsent(key) { key -> createAtlas("Reflection", key, reflectionIdConverter, resources) }.texture
    }

    @JvmStatic
    fun getTransmission(key: Identifier, resources: ResourceManager = Minecraft.getInstance().resourceManager): GpuTexture {
        return transmission.computeIfAbsent(key) { key -> createAtlas("Transmission", key, transmissionIdConverter, resources) }.texture
    }
}